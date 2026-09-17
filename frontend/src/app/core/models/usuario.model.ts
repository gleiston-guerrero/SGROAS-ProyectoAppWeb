export interface Usuario {
  id: number;
  name: string;
  email: string;
  role: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UsuarioRequest {
  name: string;
  email: string;
  password: string;
  role: string;
}
