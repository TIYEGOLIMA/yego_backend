import { IsNumber, IsOptional, IsArray } from 'class-validator';
import { ApiPropertyOptional } from '@nestjs/swagger';

export class ProcessImportDto {
  @ApiPropertyOptional({ description: 'IDs de filas a procesar' })
  @IsOptional()
  @IsArray()
  rowIds?: number[];

  @ApiPropertyOptional({ description: 'Saltar validación' })
  @IsOptional()
  skipValidation?: boolean;
} 