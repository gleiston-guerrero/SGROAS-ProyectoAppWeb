import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { catchError, EMPTY } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, MensajeResponse, Sesion } from '../models/auth.model';

const SESION_KEY = 'sgroas_sesion';

@Injectable({
  providedIn: 'root'
})
export class Auth {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  currentUser = signal<Sesion | null>(this.cargarLocal());

  constructor(private http: HttpClient) {
    this.validarSesionConServidor();
  }

  login(credentials: LoginRequest): Observable<Sesion> {
    return this.http
      .post<Sesion>(`${this.apiUrl}/login`, credentials, {
        withCredentials: true
      })
      .pipe(tap((response) => this.guardarSesion(response)));
  }

  /** Confirma el codigo de 6 digitos y deja la sesion iniciada. */
  verificarEmail(email: string, codigo: string): Observable<Sesion> {
    return this.http
      .post<Sesion>(
        `${this.apiUrl}/verify-email`,
        { email, codigo },
        { withCredentials: true }
      )
      .pipe(tap((response) => this.guardarSesion(response)));
  }

  reenviarCodigo(email: string): Observable<MensajeResponse> {
    return this.http.post<MensajeResponse>(`${this.apiUrl}/resend-code`, { email });
  }

  olvidarContrasena(email: string): Observable<MensajeResponse> {
    return this.http.post<MensajeResponse>(`${this.apiUrl}/forgot-password`, { email });
  }

  restablecerContrasena(
    email: string,
    codigo: string,
    nuevaPassword: string
  ): Observable<MensajeResponse> {
    return this.http.post<MensajeResponse>(`${this.apiUrl}/reset-password`, {
      email,
      codigo,
      nuevaPassword
    });
  }

  logout(): Observable<void> {
    // La sesion local se limpia de inmediato: el cierre nunca queda bloqueado
    // si la peticion al servidor falla o responde con error.
    // Los tokens viajan en cookies HttpOnly: no se envian en el cuerpo.
    this.limpiarSesion();
    return this.http
      .post<void>(`${this.apiUrl}/logout`, {}, { withCredentials: true })
      .pipe(catchError(() => EMPTY));
  }

  isAuthenticated(): boolean {
    return this.currentUser() !== null;
  }

  rolActual(): string | null {
    const rol = this.currentUser()?.role;
    return rol ? rol.replace('ROLE_', '') : null;
  }

  tieneRol(roles: string[]): boolean {
    const rol = this.rolActual();
    return rol !== null && roles.includes(rol);
  }

  private guardarSesion(response: Sesion): void {
    this.currentUser.set(response);
    try {
      // Solo el perfil (sin JWT): los tokens viven en cookies HttpOnly.
      localStorage.setItem(SESION_KEY, JSON.stringify(response));
    } catch { /* almacenamiento no disponible */ }
  }

  private limpiarSesion(): void {
    this.currentUser.set(null);
    try {
      localStorage.removeItem(SESION_KEY);
    } catch { /* almacenamiento no disponible */ }
  }

  private cargarLocal(): Sesion | null {
    try {
      const bruto = localStorage.getItem(SESION_KEY);
      if (!bruto) return null;
      const sesion = JSON.parse(bruto) as Sesion;
      // Defensa contra sesiones cacheadas con el contrato viejo (campos en
      // espanol `rol`/`nombre` en vez de `role`/`name`, de antes del fix del
      // contrato backend-frontend): si falta `role`, la sesion guardada no
      // sirve con el codigo actual (el menu por rol quedaria vacio en
      // silencio). Se descarta y se fuerza un login nuevo en vez de operar
      // con datos incompletos.
      if (!sesion || typeof sesion.role !== 'string' || !sesion.role) {
        localStorage.removeItem(SESION_KEY);
        return null;
      }
      return sesion;
    } catch {
      return null;
    }
  }

  /** Confirma contra el backend que la cookie HttpOnly sigue valida
   *  (al recargar la pagina el signal se restaura del localStorage y
   *  aqui se descarta si la sesion ya expiro). */
  private validarSesionConServidor(): void {
    if (this.currentUser() === null) return;
    this.http
      .get<Sesion>(`${this.apiUrl}/me`, { withCredentials: true })
      .subscribe({
        next: (response) => this.guardarSesion(response),
        error: () => this.limpiarSesion()
      });
  }
}
