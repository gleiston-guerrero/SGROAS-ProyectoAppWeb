import { Component, signal, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { Auth } from '../../../core/services/auth';

type Vista = 'acceso' | 'verificar' | 'olvidar' | 'restablecer';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login implements OnDestroy {
  private fb = inject(FormBuilder);
  private authService = inject(Auth);
  private router = inject(Router);

  loading = signal(false);
  errorMsg = signal<string | null>(null);
  infoMsg = signal<string | null>(null);
  vista = signal<Vista>('acceso');
  cooldown = signal(0);
  private temporizador: ReturnType<typeof setInterval> | null = null;

  // Nota de auditoria (2026-09-16): las credenciales de las cuentas de
  // demostracion se retiraron del codigo versionado. `usarCuenta` ya no
  // rellena la contrasena automaticamente: el usuario debe teclearla.
  cuentasDemo = [
    { email: 'admin@sgroas.com', rol: 'Administrador', detalle: 'Acceso total al sistema' },
    { email: 'coordinador@sgroas.com', rol: 'Coordinador', detalle: 'Flota, unidades, rutas y programaciones' },
    { email: 'seguridad@sgroas.com', rol: 'Seguridad', detalle: 'Incidentes y alertas' },
  ];

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  verificarForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    codigo: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });

  olvidarForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  restablecerForm = this.fb.group({
    codigo: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    nuevaPassword: ['', [Validators.required, Validators.minLength(6)]]
  });

  ngOnDestroy(): void {
    this.detenerCooldown();
  }

  irA(vista: Vista): void {
    this.errorMsg.set(null);
    this.infoMsg.set(null);
    if (vista === 'verificar') {
      const email =
        this.verificarForm.getRawValue().email || this.loginForm.getRawValue().email;
      this.verificarForm.patchValue({ email });
      this.iniciarCooldown();
    }
    if (vista === 'restablecer') {
      this.restablecerForm.patchValue({
        codigo: '',
        nuevaPassword: ''
      });
    }
    this.vista.set(vista);
  }

  usarCuenta(cuenta: { email: string }): void {
    this.loginForm.patchValue({ email: cuenta.email });
    this.loginForm.get('password')?.setValue('');
    this.loginForm.get('password')?.markAsUntouched();
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMsg.set(null);

    this.authService.login(this.loginForm.getRawValue() as { email: string; password: string }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 403) {
          // Cuenta sin verificar: pedimos directamente el codigo de activacion.
          this.verificarForm.patchValue({ email: this.loginForm.getRawValue().email, codigo: '' });
          this.errorMsg.set(null);
          this.infoMsg.set(
            'Tu cuenta aun no esta activada. Ingresa el codigo de 6 digitos que enviamos a tu correo.'
          );
          this.iniciarCooldown();
          this.vista.set('verificar');
          return;
        }
        this.errorMsg.set(
          err.status === 401 || err.status === 400
            ? 'Correo o contraseña incorrectos.'
            : 'No se pudo conectar con el servidor. Intenta de nuevo.'
        );
      }
    });
  }

  verificar(): void {
    if (this.verificarForm.invalid) {
      this.verificarForm.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMsg.set(null);

    const { email, codigo } = this.verificarForm.getRawValue();
    this.authService.verificarEmail(email ?? '', codigo ?? '').subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => this.manejarError(err)
    });
  }

  reenviar(): void {
    if (this.cooldown() > 0) return;
    const email = this.verificarForm.getRawValue().email;
    if (!email) {
      this.verificarForm.get('email')?.markAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMsg.set(null);

    this.authService.reenviarCodigo(email).subscribe({
      next: (respuesta) => {
        this.loading.set(false);
        this.infoMsg.set(respuesta.mensaje);
        this.iniciarCooldown();
      },
      error: (err) => this.manejarError(err)
    });
  }

  solicitarRestablecimiento(): void {
    if (this.olvidarForm.invalid) {
      this.olvidarForm.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMsg.set(null);

    const email = this.olvidarForm.getRawValue().email;
    this.authService.olvidarContrasena(email ?? '').subscribe({
      next: (respuesta) => {
        this.loading.set(false);
        this.iniciarCooldown();
        this.infoMsg.set(respuesta.mensaje);
        this.vista.set('restablecer');
      },
      error: (err) => this.manejarError(err)
    });
  }

  restablecer(): void {
    if (this.restablecerForm.invalid) {
      this.restablecerForm.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.errorMsg.set(null);

    const email = this.olvidarForm.getRawValue().email;
    const { codigo, nuevaPassword } = this.restablecerForm.getRawValue();
    this.authService
      .restablecerContrasena(email ?? '', codigo ?? '', nuevaPassword ?? '')
      .subscribe({
      next: (respuesta) => {
        this.loading.set(false);
        this.loginForm.patchValue({ email });
        this.restablecerForm.reset();
        this.olvidarForm.reset();
        this.infoMsg.set(respuesta.mensaje);
        this.vista.set('acceso');
      },
      error: (err) => this.manejarError(err)
    });
  }

  private iniciarCooldown(): void {
    this.detenerCooldown();
    this.cooldown.set(60);
    this.temporizador = setInterval(() => {
      const restante = this.cooldown() - 1;
      this.cooldown.set(restante);
      if (restante <= 0) this.detenerCooldown();
    }, 1000);
  }

  private detenerCooldown(): void {
    if (this.temporizador !== null) {
      clearInterval(this.temporizador);
      this.temporizador = null;
    }
  }

  private manejarError(err: unknown): void {
    this.loading.set(false);
    const detalle = (err as { error?: { detail?: string } })?.error?.detail;
    if (detalle) {
      this.errorMsg.set(detalle);
    } else {
      this.errorMsg.set('No se pudo completar la operacion. Intenta de nuevo.');
    }
  }
}
