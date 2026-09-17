import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subject, interval, takeUntil } from 'rxjs';
import { finalize } from 'rxjs';
import { Auth } from '../../../core/services/auth';
import { AlertaAbdService } from '../../../core/services/abd/alerta-abd.service';
import { AlertaAbd } from '../../../core/models/abd.model';

interface NavItem {
  label: string;
  path: string;
  roles: string[] | null;
}

const VISTA_KEY = 'sgroas_alerta_vista';
const ETIQUETAS_ROL: Record<string, string> = {
  ADMIN: 'Administrador',
  COORDINADOR: 'Coordinador',
  SEGURIDAD: 'Seguridad',
  OPERADOR: 'Operador',
};

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell.html',
  styleUrl: './shell.scss'
})
export class Shell implements OnInit, OnDestroy {
  private authService = inject(Auth);
  private alertaService = inject(AlertaAbdService);
  private router = inject(Router);
  private destruir$ = new Subject<void>();

  private readonly todosLosItems: NavItem[] = [
    { label: 'Inicio', path: '', roles: null },
    { label: 'Usuarios y Roles', path: 'usuarios', roles: ['ADMIN'] },
    { label: 'Flota Vehicular', path: 'flota', roles: ['ADMIN', 'COORDINADOR'] },
    { label: 'Unidades', path: 'unidades', roles: ['ADMIN', 'COORDINADOR'] },
    { label: 'Rutas', path: 'rutas', roles: ['ADMIN', 'COORDINADOR'] },
    { label: 'Programaciones', path: 'programaciones', roles: ['ADMIN', 'COORDINADOR'] },
    { label: 'Seguridad', path: 'seguridad', roles: ['ADMIN', 'SEGURIDAD'] },
    { label: 'Reportes', path: 'reportes', roles: null }
  ];

  alertas = signal<AlertaAbd[]>([]);
  nuevasCount = signal(0);
  panelAbierto = signal(false);

  ngOnInit(): void {
    this.cargarAlertas();
    interval(60000)
      .pipe(takeUntil(this.destruir$))
      .subscribe(() => this.cargarAlertas());
  }

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
  }

  get navItems(): NavItem[] {
    return this.todosLosItems.filter(
      (item) => item.roles === null || this.authService.tieneRol(item.roles)
    );
  }

  get currentUser() {
    return this.authService.currentUser();
  }

  get rolEtiqueta(): string {
    const rol = this.authService.rolActual();
    return rol ? (ETIQUETAS_ROL[rol] ?? rol) : '';
  }

  get initials(): string {
    const name = this.currentUser?.name ?? '';
    return name
      .split(' ')
      .map((p) => p[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }

  alternarPanel(): void {
    this.panelAbierto.set(!this.panelAbierto());
    if (this.panelAbierto()) {
      this.cargarAlertas();
    }
  }

  marcarComoVistas(): void {
    const maxima = Math.max(0, ...this.alertas().map((a) => a.idAlerta));
    try {
      localStorage.setItem(VISTA_KEY, String(maxima));
    } catch { /* almacenamiento no disponible */ }
    this.nuevasCount.set(0);
  }

  cargarAlertas(): void {
    this.alertaService.ultimas().subscribe({
      next: (res) => {
        this.alertas.set(res.content as AlertaAbd[]);
        this.recalcularNuevas();
      },
    });
  }

  private recalcularNuevas(): void {
    let vista = 0;
    try {
      vista = Number(localStorage.getItem(VISTA_KEY) ?? 0);
    } catch { vista = 0; }
    if (!vista) {
      // Primera vez: no marcar todo como nuevo, tomar la mas reciente como referencia
      const maxima = Math.max(0, ...this.alertas().map((a) => a.idAlerta));
      try {
        localStorage.setItem(VISTA_KEY, String(maxima));
      } catch { /* ignorado */ }
      this.nuevasCount.set(0);
      return;
    }
    this.nuevasCount.set(this.alertas().filter((a) => a.idAlerta > vista).length);
  }

  logout(): void {
    // finalize garantiza la navegacion al login aunque la peticion falle
    this.authService
      .logout()
      .pipe(finalize(() => this.router.navigate(['/login'])))
      .subscribe();
  }
}
