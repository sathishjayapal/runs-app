export class StravaRunDTO {

  constructor(data:Partial<StravaRunDTO>) {
    Object.assign(this, data);
  }

  runNumber?: number|null;
  customerId?: number|null;
  runName?: string|null;
  runDate?: string|null;
  miles?: number|null;
  startLocation?: number|null;
  createdAt?: string|null;
  updatedAt?: string|null;
  updatedBy?: string|null;
  createdBy?: number|null;

}
