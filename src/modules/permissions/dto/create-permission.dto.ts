import { IsString, IsNotEmpty, IsOptional, IsBoolean, IsObject } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreatePermissionDto {
  @ApiProperty({ description: 'Nombre del permiso' })
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiPropertyOptional({ description: 'Descripción del permiso' })
  @IsOptional()
  @IsString()
  description?: string;

  @ApiProperty({ description: 'Módulo al que pertenece el permiso' })
  @IsString()
  @IsNotEmpty()
  module: string;

  @ApiProperty({ description: 'Acción del permiso' })
  @IsString()
  @IsNotEmpty()
  action: string;

  @ApiPropertyOptional({ description: 'Condiciones adicionales del permiso' })
  @IsOptional()
  @IsObject()
  conditions?: Record<string, any>;

  @ApiPropertyOptional({ description: 'Si el permiso está activo', default: true })
  @IsOptional()
  @IsBoolean()
  active?: boolean;
} 