import { IsString, IsNotEmpty, IsOptional, IsBoolean, IsArray, IsNumber, IsObject } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateModuleDto {
  @ApiProperty({ description: 'Nombre del módulo' })
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiPropertyOptional({ description: 'Descripción del módulo' })
  @IsOptional()
  @IsString()
  description?: string;

  @ApiProperty({ description: 'Ruta del módulo' })
  @IsString()
  @IsNotEmpty()
  route: string;

  @ApiPropertyOptional({ description: 'Icono del módulo' })
  @IsOptional()
  @IsString()
  icon?: string;

  @ApiPropertyOptional({ description: 'Orden del módulo', default: 0 })
  @IsOptional()
  @IsNumber()
  order?: number;

  @ApiPropertyOptional({ description: 'Si el módulo está activo', default: true })
  @IsOptional()
  @IsBoolean()
  active?: boolean;

  @ApiPropertyOptional({ description: 'Permisos requeridos para el módulo' })
  @IsOptional()
  @IsArray()
  permissions?: string[];

  @ApiPropertyOptional({ description: 'Configuración adicional del módulo' })
  @IsOptional()
  @IsObject()
  config?: Record<string, any>;
} 