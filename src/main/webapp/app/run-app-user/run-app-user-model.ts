export class RunAppUserDTO {

  constructor(data:Partial<RunAppUserDTO>) {
    Object.assign(this, data);
  }

  id?: number|null;
  email?: string|null;
  password?: string|null;
  name?: string|null;
  roles?: number[]|null;

}
