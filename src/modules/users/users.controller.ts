import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards, Query, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiQuery } from '@nestjs/swagger';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { UserResponseDto } from './dto/user-response.dto';
import { CreateUserExampleDto } from './dto/create-user-example.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { DniValidationService } from '../shared/services/dni-validation.service';

@ApiTags('Usuarios')
@Controller('users')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class UsersController {
  constructor(
    private readonly usersService: UsersService,
    private readonly dniValidationService: DniValidationService
  ) {}

  @Get('profile')
  @ApiOperation({ summary: 'Obtener perfil del usuario actual' })
  @ApiResponse({ status: 200, description: 'Perfil del usuario' })
  getProfile(@Request() req) {
    return req.user;
  }

  @Post()
  @ApiOperation({ 
    summary: 'Crear nuevo usuario',
    description: 'Crea un nuevo usuario. Se recomienda usar firstName y lastName en lugar de nombre para mejor organización de datos.'
  })
  @ApiResponse({ 
    status: 201, 
    description: 'Usuario creado exitosamente',
    type: UserResponseDto
  })
  @ApiResponse({ status: 409, description: 'Usuario o email ya existe' })
  create(@Body() createUserDto: CreateUserDto) {
    return this.usersService.create(createUserDto);
  }

  @Post('example')
  @ApiOperation({ 
    summary: 'Ejemplo de creación de usuario',
    description: 'Ejemplo de cómo crear un usuario usando los nuevos campos firstName y lastName'
  })
  @ApiResponse({ 
    status: 201, 
    description: 'Usuario creado exitosamente',
    type: UserResponseDto
  })
  createExample(@Body() createUserDto: CreateUserExampleDto) {
    return this.usersService.create(createUserDto);
  }

  @Get()
  @ApiOperation({ summary: 'Listar usuarios' })
  @ApiResponse({ 
    status: 200, 
    description: 'Lista de usuarios obtenida',
    type: [UserResponseDto]
  })
  @ApiQuery({ name: 'page', required: false, type: Number, description: 'Número de página' })
  @ApiQuery({ name: 'limit', required: false, type: Number, description: 'Elementos por página' })
  @ApiQuery({ name: 'search', required: false, type: String, description: 'Término de búsqueda' })
  @ApiQuery({ name: 'activo', required: false, type: String, description: 'Filtrar por usuarios activos (true), inactivos (false) o todos (all)' })
  findAll(
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('search') search?: string,
    @Query('activo') activo?: string,
  ) {
    const pageNum = page ? parseInt(page, 10) : 1;
    const limitNum = limit ? parseInt(limit, 10) : 10;
    let activoParam: boolean | null = true;
    if (activo === 'false') activoParam = false;
    if (activo === 'all' || activo === null) activoParam = null;
    return this.usersService.findAll(pageNum, limitNum, search, activoParam);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Obtener usuario por ID' })
  @ApiResponse({ 
    status: 200, 
    description: 'Usuario encontrado',
    type: UserResponseDto
  })
  @ApiResponse({ status: 404, description: 'Usuario no encontrado' })
  findOne(@Param('id') id: number) {
    return this.usersService.findOne(id);
  }

  @Patch(':id')
  @ApiOperation({ 
    summary: 'Actualizar usuario',
    description: 'Actualiza un usuario existente. Si se proporcionan firstName y lastName, se actualizará automáticamente el campo nombre.'
  })
  @ApiResponse({ 
    status: 200, 
    description: 'Usuario actualizado exitosamente',
    type: UserResponseDto
  })
  @ApiResponse({ status: 404, description: 'Usuario no encontrado' })
  update(@Param('id') id: number, @Body() updateUserDto: UpdateUserDto) {
    return this.usersService.update(id, updateUserDto);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Eliminar usuario' })
  @ApiResponse({ status: 200, description: 'Usuario eliminado exitosamente' })
  @ApiResponse({ status: 404, description: 'Usuario no encontrado' })
  remove(@Param('id') id: number) {
    return this.usersService.remove(id);
  }

  @Patch(':id/activate')
  @ApiOperation({ summary: 'Activar usuario' })
  @ApiResponse({ status: 200, description: 'Usuario activado exitosamente' })
  activate(@Param('id') id: number) {
    return this.usersService.activate(id);
  }

  @Patch(':id/deactivate')
  @ApiOperation({ summary: 'Desactivar usuario' })
  @ApiResponse({ status: 200, description: 'Usuario desactivado exitosamente' })
  deactivate(@Param('id') id: number) {
    return this.usersService.deactivate(id);
  }

  @Patch(':id/change-password')
  @ApiOperation({ summary: 'Cambiar contraseña de usuario' })
  @ApiResponse({ status: 200, description: 'Contraseña cambiada exitosamente' })
  changePassword(
    @Param('id') id: number,
    @Body('newPassword') newPassword: string,
  ) {
    return this.usersService.changePassword(id, newPassword);
  }

  @Get('validate-dni/:dni')
  @ApiOperation({ summary: 'Validar DNI con API externa' })
  @ApiResponse({ status: 200, description: 'DNI validado exitosamente' })
  @ApiResponse({ status: 404, description: 'DNI no encontrado' })
  async validateDni(@Param('dni') dni: string) {
    const dniData = await this.dniValidationService.validateDni(dni);
    
    if (dniData) {
      const formattedData = this.dniValidationService.formatUserData(dniData);
      return {
        success: true,
        data: formattedData,
        message: 'DNI validado exitosamente'
      };
    } else {
      return {
        success: false,
        message: 'DNI no encontrado o error en la validación'
      };
    }
  }
} 