import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { ModulesService } from './modules.service';
import { CreateModuleDto } from './dto/create-module.dto';
import { UpdateModuleDto } from './dto/update-module.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@ApiTags('Modules')
@Controller('modules')
@UseGuards(JwtAuthGuard, RolesGuard)
@ApiBearerAuth()
export class ModulesController {
  constructor(private readonly modulesService: ModulesService) {}

  @Post()
  @ApiOperation({ summary: 'Crear nuevo módulo' })
  @ApiResponse({ status: 201, description: 'Módulo creado exitosamente' })
  @ApiResponse({ status: 400, description: 'Datos inválidos' })
  @ApiResponse({ status: 409, description: 'El módulo ya existe' })
  @Roles('admin')
  async create(@Body() createModuleDto: CreateModuleDto) {
    return this.modulesService.create(createModuleDto);
  }

  @Get()
  @ApiOperation({ summary: 'Obtener todos los módulos activos' })
  @ApiResponse({ status: 200, description: 'Lista de módulos activos obtenida exitosamente' })
  @Roles('admin', 'supervisor')
  async findAll() {
    return this.modulesService.findAll();
  }

  @Get('all')
  @ApiOperation({ summary: 'Obtener todos los módulos (activos e inactivos)' })
  @ApiResponse({ status: 200, description: 'Lista de todos los módulos obtenida exitosamente' })
  @Roles('admin')
  async findAllIncludingInactive() {
    return this.modulesService.findAllIncludingInactive();
  }

  @Get('active')
  @ApiOperation({ summary: 'Obtener módulos activos' })
  @ApiResponse({ status: 200, description: 'Lista de módulos activos obtenida exitosamente' })
  @Roles('admin', 'supervisor')
  async findActive() {
    return this.modulesService.findActive();
  }

  @Get(':id')
  @ApiOperation({ summary: 'Obtener módulo por ID' })
  @ApiResponse({ status: 200, description: 'Módulo encontrado exitosamente' })
  @ApiResponse({ status: 404, description: 'Módulo no encontrado' })
  @Roles('admin', 'supervisor')
  async findOne(@Param('id') id: string) {
    return this.modulesService.findOne(+id);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Actualizar módulo' })
  @ApiResponse({ status: 200, description: 'Módulo actualizado exitosamente' })
  @ApiResponse({ status: 404, description: 'Módulo no encontrado' })
  @ApiResponse({ status: 409, description: 'El nombre del módulo ya existe' })
  @Roles('admin')
  async update(
    @Param('id') id: string,
    @Body() updateModuleDto: UpdateModuleDto,
  ) {
    return this.modulesService.update(+id, updateModuleDto);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Eliminar módulo' })
  @ApiResponse({ status: 200, description: 'Módulo eliminado exitosamente' })
  @ApiResponse({ status: 404, description: 'Módulo no encontrado' })
  @Roles('admin')
  async remove(@Param('id') id: string) {
    return this.modulesService.remove(+id);
  }

  @Post('register')
  @ApiOperation({ summary: 'Registrar módulo dinámicamente' })
  @ApiResponse({ status: 201, description: 'Módulo registrado exitosamente' })
  @Roles('admin')
  async registerModule(@Body() createModuleDto: CreateModuleDto) {
    return this.modulesService.registerModule(createModuleDto);
  }

  @Delete('unregister/:name')
  @ApiOperation({ summary: 'Desregistrar módulo dinámicamente' })
  @ApiResponse({ status: 200, description: 'Módulo desregistrado exitosamente' })
  @Roles('admin')
  async unregisterModule(@Param('name') name: string) {
    return this.modulesService.unregisterModule(name);
  }
} 