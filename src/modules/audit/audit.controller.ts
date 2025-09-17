import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Query,
  UseGuards,
  Request,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiQuery } from '@nestjs/swagger';
import { AuditService } from './audit.service';
import { CreateAuditLogDto } from './dto/create-audit-log.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@ApiTags('Auditoría')
@Controller('audit')
@UseGuards(JwtAuthGuard, RolesGuard)
@ApiBearerAuth()
export class AuditController {
  constructor(private readonly auditService: AuditService) {}

  @Post()
  @ApiOperation({ summary: 'Crear log de auditoría' })
  @ApiResponse({ status: 201, description: 'Log de auditoría creado exitosamente' })
  @ApiResponse({ status: 400, description: 'Datos inválidos' })
  @Roles('admin')
  async create(@Body() createAuditLogDto: CreateAuditLogDto, @Request() req) {
    return this.auditService.create(createAuditLogDto, req.user.id);
  }

  @Get()
  @ApiOperation({ summary: 'Obtener logs de auditoría con filtros' })
  @ApiResponse({ status: 200, description: 'Lista de logs de auditoría' })
  @ApiQuery({ name: 'page', required: false, type: Number })
  @ApiQuery({ name: 'limit', required: false, type: Number })
  @ApiQuery({ name: 'userId', required: false, type: Number })
  @ApiQuery({ name: 'action', required: false, type: String })
  @ApiQuery({ name: 'resource', required: false, type: String })
  @ApiQuery({ name: 'startDate', required: false, type: String })
  @ApiQuery({ name: 'endDate', required: false, type: String })
  @ApiQuery({ name: 'search', required: false, type: String })
  @Roles('admin', 'supervisor')
  async findAll(
    @Query('page') page: string = '1',
    @Query('limit') limit: string = '50',
    @Query('userId') userId?: string,
    @Query('action') action?: string,
    @Query('resource') resource?: string,
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
    @Query('search') search?: string,
  ) {
    const filters = {
      userId: userId ? parseInt(userId) : undefined,
      action,
      resource,
      startDate: startDate ? new Date(startDate) : undefined,
      endDate: endDate ? new Date(endDate) : undefined,
      search,
    };

    return this.auditService.findAll(parseInt(page), parseInt(limit), filters);
  }

  @Get('stats')
  @ApiOperation({ summary: 'Obtener estadísticas de auditoría' })
  @ApiResponse({ status: 200, description: 'Estadísticas de auditoría' })
  @ApiQuery({ name: 'days', required: false, type: Number, description: 'Días hacia atrás (default: 30)' })
  @Roles('admin')
  async getStats(@Query('days') days: string = '30') {
    return this.auditService.getStats(parseInt(days));
  }

  @Get('recent')
  @ApiOperation({ summary: 'Obtener actividad reciente' })
  @ApiResponse({ status: 200, description: 'Actividad reciente' })
  @ApiQuery({ name: 'limit', required: false, type: Number })
  @Roles('admin', 'supervisor')
  async getRecentActivity(@Query('limit') limit: string = '20') {
    return this.auditService.getRecentActivity(parseInt(limit));
  }

  @Get('user/:userId')
  @ApiOperation({ summary: 'Obtener logs de un usuario específico' })
  @ApiResponse({ status: 200, description: 'Logs del usuario' })
  @ApiQuery({ name: 'limit', required: false, type: Number })
  @Roles('admin')
  async findByUser(
    @Param('userId') userId: string,
    @Query('limit') limit: string = '100',
  ) {
    return this.auditService.findByUser(parseInt(userId), parseInt(limit));
  }

  @Get('action/:action')
  @ApiOperation({ summary: 'Obtener logs por acción específica' })
  @ApiResponse({ status: 200, description: 'Logs de la acción' })
  @ApiQuery({ name: 'limit', required: false, type: Number })
  @Roles('admin')
  async findByAction(
    @Param('action') action: string,
    @Query('limit') limit: string = '100',
  ) {
    return this.auditService.findByAction(action, parseInt(limit));
  }

  @Get('resource/:resource')
  @ApiOperation({ summary: 'Obtener logs por recurso específico' })
  @ApiResponse({ status: 200, description: 'Logs del recurso' })
  @ApiQuery({ name: 'limit', required: false, type: Number })
  @Roles('admin')
  async findByResource(
    @Param('resource') resource: string,
    @Query('limit') limit: string = '100',
  ) {
    return this.auditService.findByResource(resource, parseInt(limit));
  }

  @Get(':id')
  @ApiOperation({ summary: 'Obtener log específico por ID' })
  @ApiResponse({ status: 200, description: 'Log encontrado' })
  @ApiResponse({ status: 404, description: 'Log no encontrado' })
  @Roles('admin')
  async findOne(@Param('id') id: string) {
    return this.auditService.findOne(parseInt(id));
  }
} 