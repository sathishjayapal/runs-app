export class ShedlockDTO {

  constructor(data:Partial<ShedlockDTO>) {
    Object.assign(this, data);
  }

  name?: number|null;
  lockUntil?: string|null;
  lockedAt?: string|null;
  lockedBy?: string|null;

}
