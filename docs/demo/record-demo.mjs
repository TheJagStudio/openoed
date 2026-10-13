// OpenOED one-minute demo recorder.
// Prereq: .\dev.ps1 up  (web :4206, api :8087). Then:
//   npm install
//   node record-demo.mjs
//   .\encode.ps1
import { chromium } from 'playwright';
import fs from 'node:fs';

const BASE = 'http://localhost:4206';
const API = 'http://localhost:8087/api/v1';
const W = 1920, H = 1080;

async function api(method, path, token, body) {
  const r = await fetch(API + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!r.ok) throw new Error(`${method} ${path} -> ${r.status} ${await r.text()}`);
  return r.status === 204 ? null : r.json();
}
const login = async (email, password) => (await api('POST', '/auth/login', null, { email, password })).accessToken;

const ex = await login('examiner@openoed.local', 'Examiner#12345');
const apps = await api('GET', '/applications', ex);
const lumen = apps.find((a) => a.mark === 'AURORA LUMEN');
const coffee = apps.find((a) => a.mark === 'NORTHWIND COFFEE');
if (!lumen || !coffee) throw new Error('seed filings missing');
if (coffee.status === 'SUBMITTED') {
  await api('POST', `/applications/${coffee.id}/office-actions`, ex, {
    type: 'NONFINAL',
    body: 'The specimen does not show the mark used in commerce. A response is due in six months.',
  });
  await new Promise((r) => setTimeout(r, 4000));
}

const browser = await chromium.launch({ channel: 'chrome', args: ['--disable-dev-shm-usage'] });
const ctx = await browser.newContext({
  viewport: { width: W, height: H },
  deviceScaleFactor: 1,
  colorScheme: 'light',
  recordVideo: { dir: '.', size: { width: W, height: H } },
});
const page = await ctx.newPage();
page.setDefaultTimeout(10000);
const wait = (ms) => page.waitForTimeout(ms);

await page.goto(BASE + '/', { waitUntil: 'networkidle' });
await wait(1200);
await page.screenshot({ path: 'landing.png' });

const started = Date.now();

async function caption(title, sub = '') {
  await page.evaluate(([t, s]) => {
    let el = document.getElementById('__cap');
    if (!el) {
      el = document.createElement('div');
      el.id = '__cap';
      el.style.cssText = 'position:fixed;left:50%;bottom:28px;transform:translateX(-50%);z-index:99999;' +
        'background:rgba(15,45,46,.94);color:#fff;padding:14px 26px;border-radius:14px;font:600 24px Inter,system-ui,sans-serif;' +
        'box-shadow:0 10px 36px rgba(0,0,0,.35);text-align:center;pointer-events:none;max-width:86%;border:1px solid rgba(242,193,78,.55)';
      document.body.appendChild(el);
    }
    el.innerHTML = t + (s ? `<div style="font:400 16px Inter,system-ui;opacity:.88;margin-top:4px">${s}</div>` : '');
  }, [title, sub]).catch(() => {});
}
async function go(p, title, sub) {
  await page.goto(BASE + p, { waitUntil: 'networkidle' }).catch(() => {});
  await wait(400);
  if (title) await caption(title, sub);
}
async function scroll(px, steps = 6, ms = 90) {
  for (let i = 0; i < steps; i++) { await page.mouse.wheel(0, px / steps); await wait(ms); }
}
async function step(name, fn) { try { await fn(); } catch (e) { console.log('skip', name, String(e.message || e).split('\n')[0]); } }
async function signIn(label) {
  const persona = page.getByRole('button', { name: label, exact: true });
  if (await persona.count()) await persona.click();
  await page.locator('button[type="submit"]').click();
  await page.waitForURL(/\/app/, { timeout: 12000 }).catch(() => {});
  await wait(600);
}

await step('landing', async () => {
  await go('/', 'OpenOED — trademark fraud detection and statutory deadlines', 'Spring Boot · Angular + ZardUI · PostgreSQL · local SQS, EventBridge, Lambda, SES, DynamoDB');
  await wait(2800);
  await scroll(700, 5, 100);
  await wait(1600);
});

await step('oed', async () => {
  await go('/login');
  await signIn('OED');
  await caption('OED dashboard', 'Open sign-up · priority reviews, high-risk filings, open deadlines');
  await wait(2800);
  await go('/app/queue', 'Priority review queue', 'Scores of 50 and above are routed here automatically');
  await wait(2800);
});

await step('lumen', async () => {
  await go(`/app/applications/${lumen.id}`, 'Aurora Lumen — critical', 'Shell address · machine-written goods · stripped photo metadata · suspended attorney');
  await wait(2500);
  await scroll(640, 6, 100);
  await wait(1800);
  await scroll(500, 4, 90);
  await wait(1200);
});

await step('examiner', async () => {
  await page.evaluate(() => localStorage.clear());
  await go('/login');
  await signIn('Examiner');
  await go(`/app/applications/${lumen.id}`, 'Examiner outside their art unit', 'Eden is assigned to L2. This filing is L1 — the visit is an anomaly.');
  await wait(2200);
  await go('/app/deadlines', 'Office-action clock', 'SQS event · EventBridge holds the six-month statutory date');
  await wait(1800);
  const fire = page.getByRole('button', { name: /Fire deadline/i }).first();
  if (await fire.count()) await fire.click();
  await caption('Deadline fired', 'No response on file. The Lambda marks the application abandoned and SES sends the notice.');
  await wait(2800);
  await scroll(420, 4, 80);
  await wait(1600);
});

await step('inspector', async () => {
  await page.evaluate(() => localStorage.clear());
  await go('/login');
  await signIn('Inspector');
  await go('/app/inspector', 'Inspector General ledger', 'Append-only. An examiner opening work outside their art unit is flagged.');
  await wait(1800);
  const box = page.locator('input[type="checkbox"]');
  if (await box.count()) await box.check();
  await page.getByRole('button', { name: /Query ledger/i }).click().catch(() => {});
  await wait(2600);
});

await step('outro', async () => {
  await page.evaluate(() => localStorage.clear());
  await go('/', 'OpenOED', 'Runs locally · maps onto Aurora, SQS, EventBridge Scheduler, Lambda, SES, and DynamoDB');
  await wait(2800);
});

const video = page.video();
await ctx.close();
await browser.close();
const src = await video.path();
fs.copyFileSync(src, 'raw.webm');
console.log('recorded', ((Date.now() - started) / 1000).toFixed(1), 's -> raw.webm');
