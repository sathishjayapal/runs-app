export class FileNameTrackerDTO {

  constructor(data:Partial<FileNameTrackerDTO>) {
    Object.assign(this, data);
  }

  id?: number|null;
  fileName?: string|null;
  createdAt?: string|null;
  updatedAt?: string|null;
  updatedBy?: string|null;
  createdBy?: number|null;

}
