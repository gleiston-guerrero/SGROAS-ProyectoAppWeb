import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Location } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UsuarioService } from '../../../core/services/usuario.service';

const ROLES = ['ROLE_ADMIN', 'ROLE_COORDINADOR', 'ROLE_SEGURIDAD'];

@Component({
  selector: 'app-usuario-formulario',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './usuario-formulario.html',
  styleUrl: './usuario-formulario.scss',
})
export class UsuarioFormulario implements OnInit {
  private fb = inject(FormBuilder);
  private service = inject(UsuarioService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);

  editId = signal<number | null>(null);
  loading = signal(false);
  loadingData = signal(false);
  errorMsg = signal<string | null>(null);
  roles = ROLES;

  form = this.fb.group({
    nombre: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.minLength(6)]],
    rol: ['ROLE_COORDINADOR', Validators.required],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.editId.set(id);
      this.loadingData.set(true);
      this.service.buscarPorId(id).subscribe({
        next: (u) => {
          this.form.patchValue({
            nombre: u.name,
            email: u.email,
            rol: u.role,
          });
          this.form.get('password')?.clearValidators();
          this.form.get('password')?.updateValueAndValidity();
          this.loadingData.set(false);
        },
        error: () => {
          this.errorMsg.set('Error al cargar datos del usuario.');
          this.loadingData.set(false);
        },
      });
    }
  }

  volver(): void {
    if (window.history.length > 1) {
      this.location.back();
    } else {
      this.router.navigate(['/dashboard/usuarios']);
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
    const data: any = { name: raw.nombre, email: raw.email, role: raw.rol, password: '' };
    const id = this.editId();

    if (id) {
      data.password = raw.password || '';
    } else {
      data.password = raw.password || 'cambio123';
    }

    (id ? this.service.actualizar(id, data) : this.service.crear(data)).subscribe({
      next: () => this.router.navigate(['/dashboard/usuarios']),
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.detail || err.error?.message || 'Error al guardar.');
      },
    });
  }
}
