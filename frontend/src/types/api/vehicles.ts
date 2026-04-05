export type VehicleStatusDTO = 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'MAINTENANCE';

export type VehicleCategoryDTO = 'HATCH' | 'SEDAN' | 'SUV' | 'PICKUP' | 'MINIVAN' | 'CONVERTIBLE' | 'COUPE';

export interface VehicleResponseDTO {
  id: string;
  brand: string;
  model: string;
  year: number;
  color?: string | null;
  licensePlate?: string | null;
  salePrice: number;
  costPrice?: number | null;
  mileageKm?: number | null;
  ipvaPaid: boolean;
  category: VehicleCategoryDTO;
  status: VehicleStatusDTO;
  photos: string[];
  organizationId: string;
  createdAt: string;
  updatedAt: string;
}

export interface VehicleCreateRequestDTO {
  brand: string;
  model: string;
  year: number;
  color?: string;
  licensePlate?: string;
  salePrice: number;
  costPrice?: number;
  mileageKm?: number;
  ipvaPaid?: boolean;
  category: VehicleCategoryDTO;
  status?: VehicleStatusDTO;
}

export interface VehicleUpdateRequestDTO {
  brand?: string;
  model?: string;
  year?: number;
  color?: string;
  licensePlate?: string;
  salePrice?: number;
  costPrice?: number;
  mileageKm?: number;
  ipvaPaid?: boolean;
  category?: VehicleCategoryDTO;
  status?: VehicleStatusDTO;
}
