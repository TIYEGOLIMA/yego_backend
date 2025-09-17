import { IsString, IsNotEmpty, IsIn } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class UploadImportDto {
  @ApiProperty({ description: 'Tipo de importación' })
  @IsString()
  @IsNotEmpty()
  @IsIn(['users', 'roles', 'permissions'])
  type: 'users' | 'roles' | 'permissions';

  @ApiProperty({ description: 'Nombre del archivo' })
  @IsString()
  @IsNotEmpty()
  filename: string;
} 