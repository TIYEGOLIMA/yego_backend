import { ApiProperty } from '@nestjs/swagger';

export class CreateUserExampleDto {
  @ApiProperty({
    description: 'Nombre de usuario único (se genera automáticamente)',
    example: 'juanperez1234',
  })
  username: string;

  @ApiProperty({
    description: 'Contraseña del usuario',
    example: 'Password123!',
  })
  password: string;

  @ApiProperty({
    description: 'Email del usuario',
    example: 'juan.perez@empresa.com',
  })
  email: string;

  @ApiProperty({
    description: 'DNI del usuario',
    example: '12345678',
    required: false,
  })
  dni?: string;

  @ApiProperty({
    description: 'Primer nombre del usuario (RECOMENDADO)',
    example: 'Juan',
  })
  firstName: string;

  @ApiProperty({
    description: 'Apellidos del usuario (RECOMENDADO)',
    example: 'Pérez García',
  })
  lastName: string;

  @ApiProperty({
    description: 'Teléfono del usuario',
    example: '+51 987654321',
    required: false,
  })
  telefono?: string;

  @ApiProperty({
    description: 'Dirección del usuario',
    example: 'Av. Principal 123, Lima',
    required: false,
  })
  direccion?: string;

  @ApiProperty({
    description: 'IDs de roles asignados',
    example: [1],
    required: false,
  })
  roleIds?: number[];
} 