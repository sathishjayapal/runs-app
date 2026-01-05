export class UserDTO {

  constructor(data:Partial<UserDTO>) {
    Object.assign(this, data);
  }

  id?: number|null;
  email?: string|null;
  password?: string|null;
  name?: string|null;
  role?: string|null;

}
