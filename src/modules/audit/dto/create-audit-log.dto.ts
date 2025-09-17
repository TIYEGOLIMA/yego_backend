import { IsString, IsOptional, IsObject } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class CreateAuditLogDto {
  @ApiProperty({ description: 'Acción realizada' })
  @IsString()
  action: string;

  @ApiPropertyOptional({ description: 'Recurso afectado' })
  @IsOptional()
  @IsString()
  resource?: string;

  @ApiPropertyOptional({ description: 'ID del recurso afectado' })
  @IsOptional()
  @IsString()
  resourceId?: string;

  @ApiPropertyOptional({ description: 'Detalles adicionales de la acción' })
  @IsOptional()
  @IsObject()
  details?: Record<string, any>;

  @ApiPropertyOptional({ description: 'Dirección IP del cliente' })
  @IsOptional()
  @IsString()
  ipAddress?: string;

  @ApiPropertyOptional({ description: 'User agent del navegador' })
  @IsOptional()
  @IsString()
  userAgent?: string;
} 