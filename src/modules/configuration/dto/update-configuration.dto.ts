import { IsString, IsOptional, IsIn } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class UpdateConfigurationDto {
  @ApiPropertyOptional({ description: 'Valor de la configuración' })
  @IsOptional()
  @IsString()
  value?: string;

  @ApiPropertyOptional({ description: 'Descripción de la configuración' })
  @IsOptional()
  @IsString()
  description?: string;

  @ApiPropertyOptional({ description: 'Categoría de la configuración' })
  @IsOptional()
  @IsString()
  category?: string;

  @ApiPropertyOptional({ 
    description: 'Tipo de dato',
    enum: ['string', 'number', 'boolean', 'json']
  })
  @IsOptional()
  @IsIn(['string', 'number', 'boolean', 'json'])
  type?: string;
} 