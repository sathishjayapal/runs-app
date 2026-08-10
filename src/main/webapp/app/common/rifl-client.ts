import axios from 'axios';

const RENEW_INTERVAL_MS = 10_000;
const GARMIN_RUNS_PREFIX = '/api/garminRuns';
const MUTATING_METHODS = new Set(['post', 'put', 'delete', 'patch']);

class RiflClient {
  private clientId: number | null = null;
  private seq = 0;
  private pendingSeqs = new Set<number>();
  private timer: ReturnType<typeof setInterval> | null = null;

  async open(): Promise<void> {
    const res = await axios.post<number>('/api/rifl/lease/open');
    this.clientId = res.data;
    this.seq = 0;
    this.pendingSeqs.clear();
    if (this.timer) clearInterval(this.timer);
    this.timer = setInterval(() => this.renew(), RENEW_INTERVAL_MS);
  }

  private async renew(): Promise<void> {
    if (this.clientId == null) return;
    try {
      await axios.post('/api/rifl/lease', null, {
        headers: { 'X-Client-Id': String(this.clientId) }
      });
    } catch {
      // best-effort heartbeat
    }
  }

  headersFor(url: string, method: string): { headers: Record<string, string>; seq: number } | null {
    if (this.clientId == null) return null;
    const path = (url ?? '').split('?')[0] ?? '';
    if (!path.startsWith(GARMIN_RUNS_PREFIX)) return null;
    if (!MUTATING_METHODS.has((method ?? '').toLowerCase())) return null;
    const seq = ++this.seq;
    this.pendingSeqs.add(seq);
    const firstIncomplete = Math.min(...this.pendingSeqs);
    return {
      headers: {
        'X-Client-Id': String(this.clientId),
        'X-Sequence-Number': String(seq),
        'X-First-Incomplete': String(firstIncomplete),
      },
      seq,
    };
  }

  ack(seq: number): void {
    this.pendingSeqs.delete(seq);
  }
}

export const riflClient = new RiflClient();
