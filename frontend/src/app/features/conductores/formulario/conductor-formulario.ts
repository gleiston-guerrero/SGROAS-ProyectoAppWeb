import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Location } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ConductorService } from '../../../core/services/conductor.service';

const TIPOS_LICENCIA = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2', 'D1', 'D2', 'E1', 'E2', 'G'];
const ESTADOS = ['ACTIVO', 'INACTIVO', 'SUSPENDIDO'];

@Component({
  selector: 'app-conductor-formulario',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './conductor-formulario.html',
  styleUrl: './conductor-formulario.scss',
})
export class ConductorFormulario implements OnInit {
  private fb = inject(FormBuilder);
  private service = inject(ConductorService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);

  editId = signal<number | null>(null);
  loading = signal(false);
  loadingData = signal(false);
  errorMsg = signal<string | null>(null);

  tiposLicencia = TIPOS_LICENCIA;
  estados = ESTADOS;

  form = this.fb.group({
    nombres: ['', Validators.required],
    apellidos: ['', Validators.required],
    cedula: ['', [Validators.required, Validators.pattern('^\\d{10}$')]],
    numeroLicencia: ['', Validators.required],
    tipoLicencia: ['', Validators.required],
    fechaVencimientoLicencia: ['', Validators.required],
    telefono: [''],
    email: ['', Validators.email],
    estado: ['ACTIVO', Validators.required],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.editId.set(id);
      this.loadingData.set(true);
      this.service.buscarPorId(id).subscribe({
        next: (c) => {
          this.form.patchValue({
            nombres: c.firstNames,
            apellidos: c.lastNames,
            cedula: c.nationalId,
            numeroLicencia: c.licenseNumber,
            tipoLicencia: c.licenseType,
            fechaVencimientoLicencia: c.licenseExpiry,
            telefono: c.phone,
            email: c.email,
            estado: c.status,
          });
          this.loadingData.set(false);
        },
        error: () => {
          this.errorMsg.set('Error al cargar datos del conductor.');
          this.loadingData.set(false);
        },
      });
    }
  }

  volver(): void {
    if (window.history.length > 1) {
      this.location.back();
    } else {
      this.router.navigate(['/dashboard/flota']);
    }
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMsg.set(null);
    const raw = this.form.getRawValue();
    const data: any = {
      firstNames: raw.nombres,
      lastNames: raw.apellidos,
      nationalId: raw.cedula,
      licenseNumber: raw.numeroLicencia,
      licenseType: raw.tipoLicencia,
      licenseExpiry: raw.fechaVencimientoLicencia,
      phone: raw.telefono,
      email: raw.email,
      status: raw.estado,
    };
    const id = this.editId();

    (id ? this.service.actualizar(id, data) : this.service.crear(data)).subscribe({
      next: () => this.router.navigate(['/dashboard/flota']),
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.detail || err.error?.message || 'Error al guardar.');
      },
    });
  }
}
