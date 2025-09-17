import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards, Query } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiQuery } from '@nestjs/swagger';
import { PermissionsService } from './permissions.service';
import { CreatePermissionDto } from './dto/create-permission.dto';
import { UpdatePermissionDto } from './dto/update-permission.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@ApiTags('Permissions')
@Controller('permissions')
@UseGuards(JwtAuthGuard, RolesGuard)
@ApiBearerAuth()
export class PermissionsController {
  constructor(private readonly permissionsService: PermissionsService) {}

  @Post()
  @ApiOperation({ summary: 'Crear nuevo permiso' })
  @ApiResponse({ status: 201, description: 'Permiso creado exitosamente' })
  @ApiResponse({ status: 400, description: 'Datos inválidos' })
  @ApiResponse({ status: 409, description: 'El permiso ya existe' })
  @Roles('admin')
  async create(@Body() createPermissionDto: CreatePermissionDto) {
    return this.permissionsService.create(createPermissionDto);
  }

  @Get()
  @ApiOperation({ summary: 'Obtener todos los permisos' })
  @ApiResponse({ status: 200, description: 'Lista de permisos obtenida exitosamente' })
  @ApiQuery({ name: 'module', required: false, description: 'Filtrar por módulo' })
  @Roles('admin')
  async findAll(@Query('module') module?: string) {
    if (module) {
      return this.permissionsService.findByModule(module);
    }
    return this.permissionsService.findAll();
  }

  @Get(':id')
  @ApiOperation({ summary: 'Obtener permiso por ID' })
  @ApiResponse({ status: 200, description: 'Permiso encontrado exitosamente' })
  @ApiResponse({ status: 404, description: 'Permiso no encontrado' })
  @Roles('admin')
  async findOne(@Param('id') id: string) {
    return this.permissionsService.findOne(+id);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Actualizar permiso' })
  @ApiResponse({ status: 200, description: 'Permiso actualizado exitosamente' })
  @ApiResponse({ status: 404, description: 'Permiso no encontrado' })
  @ApiResponse({ status: 409, description: 'El nombre del permiso ya existe' })
  @Roles('admin')
  async update(
    @Param('id') id: string,
    @Body() updatePermissionDto: UpdatePermissionDto,
  ) {
    return this.permissionsService.update(+id, updatePermissionDto);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Eliminar permiso' })
  @ApiResponse({ status: 200, description: 'Permiso eliminado exitosamente' })
  @ApiResponse({ status: 404, description: 'Permiso no encontrado' })
  @Roles('admin')
  async remove(@Param('id') id: string) {
    return this.permissionsService.remove(+id);
  }

  @Post('initialize')
  @ApiOperation({ summary: 'Inicializar permisos por defecto' })
  @ApiResponse({ status: 201, description: 'Permisos inicializados exitosamente' })
  @Roles('superadmin')
  async initializeDefaultPermissions() {
    await this.permissionsService.initializeDefaultPermissions();
    return {
      message: 'Permisos por defecto inicializados exitosamente',
    };
  }
} 