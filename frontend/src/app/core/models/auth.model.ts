export interface LoginRequest {
  email: string;
  password: string;
}

export interface MensajeResponse {
  mensaje: string;
}

export interface Sesion {
  name: string;
  email: string;
  role: string;
  expiresIn: number;
}
