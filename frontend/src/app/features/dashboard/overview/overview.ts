import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReporteAbdService } from '../../../core/services/abd/reporte-abd.service';
import { Auth } from '../../../core/services/auth';
import { ConteoAbd, ResumenAbd, TopRutaAbd } from '../../../core/models/abd.model';

interface SummaryCard {
  label: string;
  value: number;
  trend: string;
  alerta?: boolean;
}

interface PanelConfig {
  tag: string;
  titulo: string;
  resumen: string;
}

interface ModuleCard {
  icon: string;
  title: string;
  description: string;
  path: string;
  roles: string[] | null;
}

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './overview.html',
  styleUrl: './overview.scss'
})
export class Overview implements OnInit {
  private reportes = inject(ReporteAbdService);
  private authService = inject(Auth);

  resumen = signal<ResumenAbd | null>(null);
  unidadesEstado = signal<ConteoAbd[]>([]);
  incidentesEstado = signal<ConteoAbd[]>([]);
  incidentesNivel = signal<ConteoAbd[]>([]);
  programacionesEstado = signal<ConteoAbd[]>([]);
  topRutas = signal<TopRutaAbd[]>([]);
  loading = signal(true);
  errorMsg = signal<string | null>(null);

  rol = computed(() => this.authService.rolActual());
  userName = computed(() => this.authService.currentUser()?.name ?? '');

  panel = computed<PanelConfig>(() => {
    switch (this.rol()) {
      case 'COORDINADOR':
        return {
          tag: 'PANEL OPERATIVO',
          titulo: 'Panel del Coordinador',
          resumen: 'Tu flota, rutas y programaciones en un solo lugar.',
        };
      case 'SEGURIDAD':
        return {
          tag: 'PANEL DE SEGURIDAD',
          titulo: 'Panel de Seguridad',
          resumen: 'Incidentes y alertas bajo tu monitoreo.',
        };
      default:
        return {
          tag: 'PANEL PRINCIPAL',
          titulo: 'Panel de Administración',
          resumen: 'Resumen general del sistema de la cooperativa.',
        };
    }
  });

  private conteo(conteos: ConteoAbd[], clave: string): number {
    return conteos.find((c) => c.clave === clave)?.total ?? 0;
  }

  summary = computed<SummaryCard[]>(() => {
    const r = this.resumen();
    if (!r) return [];
    const activas = this.conteo(this.unidadesEstado(), 'Activo');
    const abiertos =
      this.conteo(this.incidentesEstado(), 'Reportado') +
      this.conteo(this.incidentesEstado(), 'En Revision');
    switch (this.rol()) {
      case 'COORDINADOR':
        return [
          {
            label: 'Unidades activas',
            value: activas,
            trend: `de ${r.totalUnidades.toLocaleString('es')} registradas`,
          },
          {
            label: 'Rutas registradas',
            value: r.totalRutas,
            trend: 'operativas en el sistema',
          },
          {
            label: 'Programaciones activas',
            value: r.programacionesActivas,
            trend: `de ${r.totalProgramaciones.toLocaleString('es')} históricas`,
          },
          {
            label: 'En mantenimiento',
            value: r.unidadesEnMantenimiento,
            trend: 'unidades en taller',
          },
          {
            label: 'Rutas más usadas',
            value: this.topRutas().length,
            trend: 'top de rutas por demanda',
          },
        ];
      case 'SEGURIDAD':
        return [
          {
            label: 'Incidentes reportados',
            value: r.totalIncidentes,
            trend: `${r.incidentesAltoNivel.toLocaleString('es')} de nivel ALTO`,
            alerta: r.incidentesAltoNivel > 0,
          },
          {
            label: 'Incidentes abiertos',
            value: abiertos,
            trend: 'sin clasificar aún',
          },
          {
            label: 'Alertas automáticas',
            value: r.totalAlertas,
            trend: 'generadas por incidentes ALTO',
            alerta: r.totalAlertas > 0,
          },
        ];
      default:
        return [
          {
            label: 'Unidades activas',
            value: activas,
            trend: `de ${r.totalUnidades.toLocaleString('es')} registradas`,
          },
          {
            label: 'Rutas registradas',
            value: r.totalRutas,
            trend: 'operativas en el sistema',
          },
          {
            label: 'Programaciones activas',
            value: r.programacionesActivas,
            trend: `de ${r.totalProgramaciones.toLocaleString('es')} históricas`,
          },
          {
            label: 'Incidentes abiertos',
            value: abiertos,
            trend: `${r.incidentesAltoNivel.toLocaleString('es')} de nivel ALTO`,
            alerta: r.incidentesAltoNivel > 0,
          },
          {
            label: 'Alertas automáticas',
            value: r.totalAlertas,
            trend: 'generadas por incidentes ALTO',
            alerta: r.totalAlertas > 0,
          },
          {
            label: 'En mantenimiento',
            value: r.unidadesEnMantenimiento,
            trend: 'unidades en taller',
          },
        ];
    }
  });

  private readonly todosLosModules: ModuleCard[] = [
    { icon: '👤', title: 'Usuarios y Roles', description: 'Administra socios, choferes y permisos de acceso.', path: 'usuarios', roles: ['ADMIN'] },
    { icon: '🚌', title: 'Flota Vehicular', description: 'Control de unidades, placas y mantenimiento.', path: 'flota', roles: ['ADMIN', 'COORDINADOR'] },
    { icon: '🛣️', title: 'Rutas y Frecuencias', description: 'Asignación de horarios y recorridos por unidad.', path: 'rutas', roles: ['ADMIN', 'COORDINADOR'] },
    { icon: '🛡️', title: 'Seguridad', description: 'Monitoreo, incidentes y alertas en tiempo real.', path: 'seguridad', roles: ['ADMIN', 'SEGURIDAD'] },
    { icon: '📅', title: 'Programaciones', description: 'Salidas diarias por ruta, unidad y conductor.', path: 'programaciones', roles: ['ADMIN', 'COORDINADOR'] },
    { icon: '📊', title: 'Reportes', description: 'Estadísticas operativas y de seguridad exportables.', path: 'reportes', roles: null }
  ];

  get modules(): ModuleCard[] {
    return this.todosLosModules.filter(
      (m) => m.roles === null || this.authService.tieneRol(m.roles)
    );
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.errorMsg.set(null);
    this.reportes.resumen().subscribe({
      next: (r) => {
        this.resumen.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.errorMsg.set('No se pudieron cargar los indicadores.');
        this.loading.set(false);
      },
    });
    this.reportes.unidadesPorEstado().subscribe({ next: (c) => this.unidadesEstado.set(c) });
    this.reportes.incidentesPorEstado().subscribe({ next: (c) => this.incidentesEstado.set(c) });
    this.reportes.incidentesPorNivel().subscribe({ next: (c) => this.incidentesNivel.set(c) });
    this.reportes.programacionesPorEstado().subscribe({ next: (c) => this.programacionesEstado.set(c) });
    this.reportes.topRutas().subscribe({ next: (c) => this.topRutas.set(c) });
  }
}
