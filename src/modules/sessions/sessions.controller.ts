import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Delete,
  UseGuards,
  Request,
  HttpCode,
  HttpStatus,
  BadRequestException,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { SessionsService } from './sessions.service';
import { CreateSessionDto } from './dto/create-session.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@ApiTags('Sesiones')
@Controller('sessions')
@UseGuards(JwtAuthGuard, RolesGuard)
@ApiBearerAuth()
export class SessionsController {
  constructor(private readonly sessionsService: SessionsService) {}

  @Post()
  @ApiOperation({ summary: 'Crear nueva sesión' })
  @ApiResponse({ status: 201, description: 'Sesión creada exitosamente' })
  @ApiResponse({ status: 400, description: 'Datos inválidos' })
  async create(@Body() createSessionDto: CreateSessionDto, @Request() req) {
    return this.sessionsService.create(createSessionDto, req.user.id, req);
  }

  @Get()
  @ApiOperation({ summary: 'Obtener todas las sesiones activas' })
  @ApiResponse({ status: 200, description: 'Lista de sesiones activas' })
  @Roles('admin', 'supervisor')
  async findAll(@Request() req) {
    // Los admins ven todas las sesiones, los supervisores solo las de su equipo
    const userId = req.user.roles?.includes('admin') ? undefined : req.user.id;
    return this.sessionsService.findAll(userId);
  }

  @Get('stats')
  @ApiOperation({ summary: 'Obtener estadísticas de sesiones' })
  @ApiResponse({ status: 200, description: 'Estadísticas de sesiones' })
  @Roles('admin')
  async getStats() {
    return this.sessionsService.getSessionStats();
  }

  @Get('websocket/stats')
  @ApiOperation({ summary: 'Obtener estadísticas de WebSocket' })
  @ApiResponse({ status: 200, description: 'Estadísticas de conexiones WebSocket' })
  @Roles('admin')
  async getWebSocketStats() {
    return this.sessionsService.getWebSocketStats();
  }

  @Get('websocket/sessions')
  @ApiOperation({ summary: 'Obtener sesiones activas de WebSocket' })
  @ApiResponse({ status: 200, description: 'Sesiones activas de WebSocket' })
  @Roles('admin')
  async getWebSocketSessions() {
    return this.sessionsService.getWebSocketSessions();
  }

  @Get('connection-logs')
  @ApiOperation({ summary: 'Obtener historial de conexiones' })
  @ApiResponse({ status: 200, description: 'Historial de conexiones' })
  @Roles('admin', 'superadmin')
  async getConnectionLogs(@Request() req) {
    try {
      console.log('🔍 Iniciando getConnectionLogs con query params:', req.query);
      const { days = 30, limit = 50, user_id, role_name } = req.query;
      
      const result = await this.sessionsService.getConnectionLogs({
        days: parseInt(days as string),
        limit: parseInt(limit as string),
        user_id: user_id ? parseInt(user_id as string) : undefined,
        role_name: role_name as string
      });
      
      console.log('✅ getConnectionLogs completado exitosamente');
      return result;
    } catch (error) {
      console.error('❌ Error en getConnectionLogs controller:', error);
      throw error;
    }
  }

  @Get('count/active')
  @ApiOperation({ summary: 'Obtener cantidad de sesiones activas' })
  @ApiResponse({ status: 200, description: 'Cantidad de sesiones activas' })
  async getActiveSessionsCount(@Request() req) {
    const userId = req.user.roles?.includes('admin') ? undefined : req.user.id;
    const count = await this.sessionsService.getActiveSessionsCount(userId);
    return { count };
  }

  @Get(':id')
  @ApiOperation({ summary: 'Obtener sesión por ID' })
  @ApiResponse({ status: 200, description: 'Sesión encontrada' })
  @ApiResponse({ status: 404, description: 'Sesión no encontrada' })
  @Roles('admin')
  async findOne(@Param('id') id: string) {
    const sessionId = parseInt(id);
    if (isNaN(sessionId)) {
      throw new BadRequestException('ID de sesión inválido');
    }
    return this.sessionsService.findOne(sessionId);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Cerrar sesión específica' })
  @ApiResponse({ status: 204, description: 'Sesión cerrada exitosamente' })
  @ApiResponse({ status: 404, description: 'Sesión no encontrada' })
  @Roles('admin')
  async deactivate(@Param('id') id: string) {
    const sessionId = parseInt(id);
    if (isNaN(sessionId)) {
      throw new BadRequestException('ID de sesión inválido');
    }
    await this.sessionsService.deactivate(sessionId);
  }

  @Delete('user/:userId')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Cerrar todas las sesiones de un usuario' })
  @ApiResponse({ status: 204, description: 'Sesiones cerradas exitosamente' })
  @Roles('admin')
  async deactivateByUserId(
    @Param('userId') userId: string,
    @Body() body: { reason?: string },
  ) {
    const userIdNum = parseInt(userId);
    if (isNaN(userIdNum)) {
      throw new BadRequestException('ID de usuario inválido');
    }
    await this.sessionsService.deactivateByUserId(userIdNum, body.reason);
  }

  @Post('cleanup')
  @ApiOperation({ summary: 'Limpiar sesiones expiradas' })
  @ApiResponse({ status: 200, description: 'Sesiones expiradas limpiadas' })
  @Roles('admin')
  async cleanupExpiredSessions() {
    const count = await this.sessionsService.cleanupExpiredSessions();
    return { message: `${count} sesiones expiradas limpiadas` };
  }

  @Post(':id/force-logout')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Forzar cierre de sesión' })
  @ApiResponse({ status: 204, description: 'Sesión cerrada forzadamente' })
  @Roles('admin', 'superadmin')
  async forceLogout(@Param('id') id: string, @Request() req) {
    const sessionId = parseInt(id);
    if (isNaN(sessionId)) {
      throw new BadRequestException('ID de sesión inválido');
    }
    await this.sessionsService.forceLogout(sessionId, req.user.id);
  }
} 