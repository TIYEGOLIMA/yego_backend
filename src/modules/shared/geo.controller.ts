import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { GeoService } from './services/geo.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@ApiTags('Geolocalización')
@Controller('geo')
@UseGuards(JwtAuthGuard, RolesGuard)
@ApiBearerAuth()
export class GeoController {
  constructor(private readonly geoService: GeoService) {}

  @Get('location')
  @ApiOperation({ summary: 'Obtener ubicación por IP' })
  @ApiResponse({ status: 200, description: 'Información de ubicación obtenida' })
  @Roles('admin')
  async getLocation(@Query('ip') ip: string) {
    if (!ip) {
      return { error: 'IP address is required' };
    }
    
    const location = await this.geoService.getLocationByIP(ip);
    return location;
  }

  @Get('cache/stats')
  @ApiOperation({ summary: 'Obtener estadísticas del cache de geolocalización' })
  @ApiResponse({ status: 200, description: 'Estadísticas del cache' })
  @Roles('admin')
  async getCacheStats() {
    return this.geoService.getCacheStats();
  }

  @Get('cache/clear')
  @ApiOperation({ summary: 'Limpiar cache de geolocalización' })
  @ApiResponse({ status: 200, description: 'Cache limpiado exitosamente' })
  @Roles('admin')
  async clearCache() {
    this.geoService.clearCache();
    return { message: 'Geolocation cache cleared successfully' };
  }
} 