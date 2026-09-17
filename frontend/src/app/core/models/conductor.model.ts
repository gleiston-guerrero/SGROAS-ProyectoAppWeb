export interface Conductor {
  id: number;
  firstNames: string;
  lastNames: string;
  nationalId: string;
  licenseNumber: string;
  licenseType: string;
  licenseExpiry: string;
  phone: string;
  email: string;
  status: string;
  active: boolean;
  licenseExpiring: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ConductorRequest {
  firstNames: string;
  lastNames: string;
  nationalId: string;
  licenseNumber: string;
  licenseType: string;
  licenseExpiry: string;
  phone: string;
  email: string;
  status: string;
}
