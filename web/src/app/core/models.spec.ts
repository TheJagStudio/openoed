import { describe, expect, it } from 'vitest';
import { bandClass, isAnomaly } from './models';

describe('risk presentation', () => {
  it('maps bands onto badge classes', () => {
    expect(bandClass('CRITICAL')).toBe('band-CRITICAL');
    expect(bandClass(null)).toBe('band-LOW');
  });

  it('treats string and boolean anomaly flags the same', () => {
    expect(isAnomaly({ anomaly: true })).toBe(true);
    expect(isAnomaly({ anomaly: 'true' })).toBe(true);
    expect(isAnomaly({ anomaly: false })).toBe(false);
  });
});
