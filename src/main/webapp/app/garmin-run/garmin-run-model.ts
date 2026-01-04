export class GarminRunDTO {

  constructor(data:Partial<GarminRunDTO>) {
    Object.assign(this, data);
  }

  id?: number|null;
  activityId?: string|null;
  activityDate?: string|null;
  activityType?: string|null;
  activityName?: string|null;
  activityDescription?: string|null;
  elapsedTime?: string|null;
  distance?: string|null;
  maxHeartRate?: string|null;
  calories?: string|null;
  createdAt?: string|null;
  updatedAt?: string|null;
  updatedBy?: string|null;
  createdBy?: number|null;

}
