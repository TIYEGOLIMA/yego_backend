import { ApiProperty } from '@nestjs/swagger';

export class UserResponseDto {
  @ApiProperty({
    description: 'ID único del usuario',
    example: 1,
  })
  id: number;

  @ApiProperty({
    description: 'Nombre de usuario único',
    example: 'juanperez1234',
  })
  username: string;

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
    description: 'Nombre completo del usuario',
    example: 'Juan Pérez García',
  })
  nombre: string;

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
    description: 'Fecha de nacimiento',
    example: '1990-01-01',
    required: false,
  })
  fechaNacimiento?: Date;

  @ApiProperty({
    description: 'Estado del usuario',
    example: true,
  })
  activo: boolean;

  @ApiProperty({
    description: 'Indica si el usuario debe cambiar su contraseña',
    example: false,
  })
  requiereCambioPassword: boolean;

  @ApiProperty({
    description: 'Roles asignados al usuario',
    example: [
      {
        id: 1,
        name: 'Admin',
        description: 'Administrador del sistema'
      }
    ],
  })
  roles: Array<{
    id: number;
    name: string;
    description: string;
  }>;

  @ApiProperty({
    description: 'Fecha de creación',
    example: '2024-01-15T10:30:00Z',
  })
  createdAt: Date;

  @ApiProperty({
    description: 'Fecha de última actualización',
    example: '2024-01-15T10:30:00Z',
  })
  updatedAt: Date;
} 