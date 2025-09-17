import { IsString, IsOptional, IsDateString, IsNumber } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateSessionDto {
  @ApiProperty({ description: 'Hash del token de sesión' })
  @IsString()
  tokenHash: string;

  @ApiPropertyOptional({ description: 'Dirección IP del cliente' })
  @IsOptional()
  @IsString()
  ipAddress?: string;

  @ApiPropertyOptional({ description: 'User agent del navegador' })
  @IsOptional()
  @IsString()
  userAgent?: string;

  @ApiPropertyOptional({ description: 'Dispositivo utilizado' })
  @IsOptional()
  @IsString()
  device?: string;

  @ApiPropertyOptional({ description: 'Navegador utilizado' })
  @IsOptional()
  @IsString()
  browser?: string;

  @ApiPropertyOptional({ description: 'Sistema operativo' })
  @IsOptional()
  @IsString()
  operatingSystem?: string;

  @ApiPropertyOptional({ description: 'Ciudad' })
  @IsOptional()
  @IsString()
  city?: string;

  @ApiPropertyOptional({ description: 'Región/Estado' })
  @IsOptional()
  @IsString()
  region?: string;

  @ApiPropertyOptional({ description: 'País' })
  @IsOptional()
  @IsString()
  country?: string;

  @ApiPropertyOptional({ description: 'Código de país' })
  @IsOptional()
  @IsString()
  countryCode?: string;

  @ApiPropertyOptional({ description: 'Latitud' })
  @IsOptional()
  @IsNumber()
  latitude?: number;

  @ApiPropertyOptional({ description: 'Longitud' })
  @IsOptional()
  @IsNumber()
  longitude?: number;

  @ApiPropertyOptional({ description: 'Zona horaria' })
  @IsOptional()
  @IsString()
  timezone?: string;

  @ApiPropertyOptional({ description: 'Proveedor de internet' })
  @IsOptional()
  @IsString()
  isp?: string;

  @ApiPropertyOptional({ description: 'Organización' })
  @IsOptional()
  @IsString()
  organization?: string;

  @ApiProperty({ description: 'Fecha de expiración de la sesión' })
  @IsDateString()
  expiresAt: string;
} 