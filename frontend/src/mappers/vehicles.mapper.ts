import { Vehicle, VehicleCategory, VehicleStatus } from '@/types';
import {
  VehicleCategoryDTO,
  VehicleCreateRequestDTO,
  VehicleResponseDTO,
  VehicleStatusDTO,
  VehicleUpdateRequestDTO,
} from '@/types/api/vehicles';

function mapStatusToViewModel(status: VehicleStatusDTO): VehicleStatus {
  switch (status) {
    case 'AVAILABLE':
      return 'disponivel';
    case 'RESERVED':
      return 'reservado';
    case 'MAINTENANCE':
      return 'manutencao';
    case 'SOLD':
      return 'vendido';
    default:
      return 'disponivel';
  }
}

function mapStatusToDTO(status: VehicleStatus): VehicleStatusDTO {
  switch (status) {
    case 'disponivel':
      return 'AVAILABLE';
    case 'reservado':
      return 'RESERVED';
    case 'manutencao':
      return 'MAINTENANCE';
    case 'vendido':
      return 'SOLD';
    case 'aguardando_documentacao':
      return 'RESERVED';
    default:
      return 'AVAILABLE';
  }
}

function mapCategoryToViewModel(category: VehicleCategoryDTO): VehicleCategory {
  switch (category) {
    case 'SUV':
      return 'SUV';
    case 'SEDAN':
      return 'Sedan';
    case 'HATCH':
      return 'Hatch';
    case 'PICKUP':
      return 'Pickup';
    case 'MINIVAN':
    case 'CONVERTIBLE':
    case 'COUPE':
      return 'Outros';
    default:
      return 'Outros';
  }
}

function mapCategoryToDTO(category: VehicleCategory): VehicleCategoryDTO {
  switch (category) {
    case 'SUV':
      return 'SUV';
    case 'Sedan':
      return 'SEDAN';
    case 'Hatch':
      return 'HATCH';
    case 'Pickup':
      return 'PICKUP';
    case 'Outros':
      return 'COUPE';
    default:
      return 'SEDAN';
  }
}

export function mapVehicleResponseToVehicle(dto: VehicleResponseDTO): Vehicle {
  return {
    id: dto.id,
    marca: dto.brand,
    modelo: dto.model,
    ano: dto.year,
    preco: dto.salePrice,
    precoCusto: dto.costPrice ?? undefined,
    status: mapStatusToViewModel(dto.status),
    categoria: mapCategoryToViewModel(dto.category),
    cor: dto.color ?? undefined,
    placa: dto.licensePlate ?? undefined,
    quilometragem: dto.mileageKm ?? undefined,
    ipvaPago: dto.ipvaPaid,
    fotos: dto.photos,
    createdAt: new Date(dto.createdAt),
    updatedAt: new Date(dto.updatedAt),
  };
}

export function mapVehicleToCreateRequest(vehicle: Partial<Vehicle>): VehicleCreateRequestDTO {
  if (!vehicle.marca || !vehicle.modelo || !vehicle.ano || !vehicle.preco || !vehicle.categoria) {
    throw new Error('Dados obrigatorios de veiculo ausentes para criacao');
  }

  return {
    brand: vehicle.marca,
    model: vehicle.modelo,
    year: vehicle.ano,
    color: vehicle.cor,
    licensePlate: vehicle.placa,
    salePrice: vehicle.preco,
    costPrice: vehicle.precoCusto,
    mileageKm: vehicle.quilometragem,
    ipvaPaid: vehicle.ipvaPago,
    category: mapCategoryToDTO(vehicle.categoria),
    status: vehicle.status ? mapStatusToDTO(vehicle.status) : undefined,
  };
}

export function mapVehicleToUpdateRequest(vehicle: Partial<Vehicle>): VehicleUpdateRequestDTO {
  return {
    brand: vehicle.marca,
    model: vehicle.modelo,
    year: vehicle.ano,
    color: vehicle.cor,
    licensePlate: vehicle.placa,
    salePrice: vehicle.preco,
    costPrice: vehicle.precoCusto,
    mileageKm: vehicle.quilometragem,
    ipvaPaid: vehicle.ipvaPago,
    category: vehicle.categoria ? mapCategoryToDTO(vehicle.categoria) : undefined,
    status: vehicle.status ? mapStatusToDTO(vehicle.status) : undefined,
  };
}
