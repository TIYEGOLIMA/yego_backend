import {
  Controller,
  Get,
  Put,
  Body,
  Param,
  Delete,
  UseGuards,
  Post,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { ConfigurationService } from './configuration.service';
import { UpdateConfigurationDto } from './dto/update-configuration.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@ApiTags('Configuración')
@Controller('configuration')
@UseGuards(JwtAuthGuard, RolesGuard)
@ApiBearerAuth()
export class ConfigurationController {
  constructor(private readonly configurationService: ConfigurationService) {}

  @Get()
  @ApiOperation({ summary: 'Obtener todas las configuraciones' })
  @ApiResponse({ status: 200, description: 'Lista de configuraciones' })
  @Roles('admin')
  async findAll() {
    return this.configurationService.findAll();
  }

  @Get('system')
  @ApiOperation({ summary: 'Obtener configuración del sistema organizada por categorías' })
  @ApiResponse({ status: 200, description: 'Configuración del sistema' })
  @Roles('admin')
  async getSystemConfig() {
    return this.configurationService.getSystemConfig();
  }

  @Get('categories')
  @ApiOperation({ summary: 'Obtener categorías de configuración' })
  @ApiResponse({ status: 200, description: 'Lista de categorías' })
  @Roles('admin')
  async getCategories() {
    return this.configurationService.getCategories();
  }

  @Get('category/:category')
  @ApiOperation({ summary: 'Obtener configuraciones por categoría' })
  @ApiResponse({ status: 200, description: 'Configuraciones de la categoría' })
  @Roles('admin')
  async findByCategory(@Param('category') category: string) {
    return this.configurationService.findByCategory(category);
  }

  @Get(':key')
  @ApiOperation({ summary: 'Obtener configuración específica por clave' })
  @ApiResponse({ status: 200, description: 'Configuración encontrada' })
  @ApiResponse({ status: 404, description: 'Configuración no encontrada' })
  @Roles('admin')
  async findOne(@Param('key') key: string) {
    return this.configurationService.findOne(key);
  }

  @Get(':key/value')
  @ApiOperation({ summary: 'Obtener valor de configuración específica' })
  @ApiResponse({ status: 200, description: 'Valor de la configuración' })
  @ApiResponse({ status: 404, description: 'Configuración no encontrada' })
  @Roles('admin')
  async getValue(@Param('key') key: string) {
    const value = await this.configurationService.getValue(key);
    return { key, value };
  }

  @Put(':key')
  @ApiOperation({ summary: 'Actualizar configuración específica' })
  @ApiResponse({ status: 200, description: 'Configuración actualizada exitosamente' })
  @ApiResponse({ status: 404, description: 'Configuración no encontrada' })
  @Roles('admin')
  async update(
    @Param('key') key: string,
    @Body() updateConfigurationDto: UpdateConfigurationDto,
  ) {
    return this.configurationService.update(key, updateConfigurationDto);
  }

  @Post(':key')
  @ApiOperation({ summary: 'Establecer valor de configuración' })
  @ApiResponse({ status: 201, description: 'Configuración establecida exitosamente' })
  @Roles('admin')
  async setValue(
    @Param('key') key: string,
    @Body() body: { value: any; type?: string },
  ) {
    return this.configurationService.setValue(key, body.value, body.type);
  }

  @Delete(':key')
  @ApiOperation({ summary: 'Eliminar configuración específica' })
  @ApiResponse({ status: 200, description: 'Configuración eliminada exitosamente' })
  @ApiResponse({ status: 404, description: 'Configuración no encontrada' })
  @Roles('admin')
  async remove(@Param('key') key: string) {
    await this.configurationService.remove(key);
    return { message: `Configuración '${key}' eliminada exitosamente` };
  }

  @Post('initialize')
  @ApiOperation({ summary: 'Inicializar configuraciones por defecto' })
  @ApiResponse({ status: 201, description: 'Configuraciones inicializadas exitosamente' })
  @Roles('admin')
  async initializeDefaultConfigs() {
    await this.configurationService.initializeDefaultConfigs();
    return { message: 'Configuraciones por defecto inicializadas exitosamente' };
  }

  @Post('cache/clear')
  @ApiOperation({ summary: 'Limpiar cache de configuraciones' })
  @ApiResponse({ status: 200, description: 'Cache limpiado exitosamente' })
  @Roles('admin')
  async clearCache() {
    this.configurationService.clearCache();
    return { message: 'Cache de configuraciones limpiado exitosamente' };
  }
} 