export type Role = 'APPLICANT' | 'EXAMINER' | 'OED' | 'IG' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  displayName: string;
  role: Role;
  artUnit?: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface ApplicationSummary {
  id: string;
  serialNumber: string;
  mark: string;
  internationalClass: number;
  artUnit: string;
  status: string;
  riskScore?: number | null;
  riskBand?: string | null;
  ownerName: string;
  filedAt: string;
}

export interface Finding {
  code: string;
  title: string;
  detail: string;
  weight: number;
  severity: string;
}

export interface ApplicationDetail extends ApplicationSummary {
  markType: string;
  goodsDescription: string;
  ownerAddress: string;
  ownerEmail: string;
  attorneyName?: string | null;
  attorneyBarNumber?: string | null;
  findings: Finding[];
  claude?: { status: string; summary?: string | null; suspicion?: string | null; flags?: string[] } | null;
  review?: { id: string; status: string; priority: number; notes?: string | null } | null;
  officeActions: OfficeAction[];
  notices: Notice[];
  timeline: { kind: string; detail: string; at: string }[];
  canRespond: boolean;
  canIssueOfficeAction: boolean;
  canReview: boolean;
  canApprove: boolean;
}

export interface OfficeAction {
  id: string;
  type: string;
  body: string;
  status: string;
  issuedAt: string;
  statutoryDueAt: string;
  scheduleFireAt: string;
  scheduleName: string;
}

export interface Notice {
  toEmail: string;
  subject: string;
  body: string;
  providerMessageId?: string | null;
  sentAt: string;
  applicationId?: string;
}

export interface Dashboard {
  openReviews: number;
  highRisk: number;
  openDeadlines: number;
  anomalies: number;
  recent: ApplicationSummary[];
  role: Role;
}

export interface AuditEvent {
  action?: string;
  outcome?: string;
  at?: string;
  actorEmail?: string;
  actorRole?: string;
  artUnit?: string;
  resourceId?: string;
  detail?: string;
  anomaly?: boolean | string;
  anomalyCode?: string;
  path?: string;
  ip?: string;
}

export const ROLE_LABEL: Record<Role, string> = {
  APPLICANT: 'Applicant',
  EXAMINER: 'Examiner',
  OED: 'OED analyst',
  IG: 'Inspector General',
  ADMIN: 'Administrator',
};

export function isAnomaly(event: AuditEvent): boolean {
  return event.anomaly === true || event.anomaly === 'true';
}

export function bandClass(band?: string | null): string {
  if (!band) return 'band-LOW';
  return `band-${band}`;
}
