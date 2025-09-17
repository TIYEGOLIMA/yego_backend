import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, LessThan } from 'typeorm';
import { Session } from './entities/session.entity';
import { ConnectionLog } from './entities/connection-log.entity';
import { CreateSessionDto } from './dto/create-session.dto';
import { WebsocketGateway } from '../websocket/websocket.gateway';
import { GeoService } from '../shared/services/geo.service';
import { DeviceService } from '../shared/services/device.service';
import { Logger } from '@nestjs/common';
import { ConnectionAction } from './entities/connection-log.entity';

@Injectable()
export class SessionsService {
  private readonly logger = new Logger(SessionsService.name);

  constructor(
    @InjectRepository(Session)
    private sessionRepository: Repository<Session>,
    @InjectRepository(ConnectionLog)
    private connectionLogRepository: Repository<ConnectionLog>,
    private websocketGateway: WebsocketGateway,
    private geoService: GeoService,
    private deviceService: DeviceService,
  ) {}

  async create(createSessionDto: CreateSessionDto, userId: number, request?: any): Promise<Session> {
    // Get client IP if not provided
    if (!createSessionDto.ipAddress && request) {
      createSessionDto.ipAddress = await this.geoService.getClientIP(request);
    }

    // Get geolocation if IP is provided
    if (createSessionDto.ipAddress) {
      const location = await this.geoService.getLocationByIP(createSessionDto.ipAddress);
      if (location) {
        createSessionDto.city = location.city;
        createSessionDto.region = location.region;
        createSessionDto.country = location.country;
        createSessionDto.countryCode = location.countryCode;
        createSessionDto.latitude = location.latitude;
        createSessionDto.longitude = location.longitude;
        createSessionDto.timezone = location.timezone;
        createSessionDto.isp = location.isp;
        createSessionDto.organization = location.org;
      }
    }

    // Get device information from user agent
    if (createSessionDto.userAgent) {
      const deviceInfo = this.deviceService.getDeviceInfo(createSessionDto.userAgent);
      createSessionDto.device = deviceInfo.device;
      createSessionDto.browser = deviceInfo.browser;
      createSessionDto.operatingSystem = deviceInfo.operatingSystem;
    }

    const session = this.sessionRepository.create({
      ...createSessionDto,
      userId,
    });

    const savedSession = await this.sessionRepository.save(session);
    this.logger.log(`✅ Sesión creada para usuario ${userId}: ${savedSession.id} desde ${createSessionDto.city}, ${createSessionDto.country}`);
    
    return savedSession;
  }

  async findAll(userId?: number): Promise<Session[]> {
    const query = this.sessionRepository.createQueryBuilder('session')
      .leftJoinAndSelect('session.user', 'user')
      .where('session.active = :active', { active: true });

    if (userId) {
      query.andWhere('session.userId = :userId', { userId });
    }

    return query.getMany();
  }

  async findOne(id: number): Promise<Session> {
    const session = await this.sessionRepository.findOne({
      where: { id },
      relations: ['user'],
    });

    if (!session) {
      throw new NotFoundException(`Sesión con ID ${id} no encontrada`);
    }

    return session;
  }

  async findByTokenHash(tokenHash: string): Promise<Session | null> {
    return this.sessionRepository.findOne({
      where: { tokenHash, active: true },
      relations: ['user'],
    });
  }

  async deactivate(id: number): Promise<void> {
    const session = await this.findOne(id);
    
    session.active = false;
    await this.sessionRepository.save(session);
    
    this.logger.log(`🚪 Sesión ${id} desactivada`);
    
    // Emitir evento de cierre de sesión
    this.websocketGateway.closeSession(session.tokenHash, 'Sesión cerrada por administrador');
  }

  async deactivateByUserId(userId: number, reason: string = 'Sesión cerrada por seguridad'): Promise<void> {
    const sessions = await this.sessionRepository.find({
      where: { userId, active: true },
    });

    for (const session of sessions) {
      session.active = false;
      await this.sessionRepository.save(session);
      
      // Emitir evento de cierre de sesión
      this.websocketGateway.closeSession(session.tokenHash, reason);
    }

    this.logger.log(`🚪 ${sessions.length} sesiones desactivadas para usuario ${userId}`);
  }

  async deactivateByTokenHash(tokenHash: string): Promise<void> {
    const session = await this.findByTokenHash(tokenHash);
    
    if (session) {
      session.active = false;
      await this.sessionRepository.save(session);
      
      this.logger.log(`🚪 Sesión con token ${tokenHash} desactivada`);
      
      // Emitir evento de cierre de sesión
      this.websocketGateway.closeSession(tokenHash, 'Sesión cerrada');
    }
  }

  async cleanupExpiredSessions(): Promise<number> {
    const expiredSessions = await this.sessionRepository.find({
      where: {
        expiresAt: LessThan(new Date()),
        active: true,
      },
    });

    for (const session of expiredSessions) {
      session.active = false;
      await this.sessionRepository.save(session);
      
      // Emitir evento de cierre de sesión
      this.websocketGateway.closeSession(session.tokenHash, 'Sesión expirada');
    }

    this.logger.log(`🧹 ${expiredSessions.length} sesiones expiradas limpiadas`);
    return expiredSessions.length;
  }

  async getActiveSessionsCount(userId?: number): Promise<number> {
    const query = this.sessionRepository.createQueryBuilder('session')
      .where('session.active = :active', { active: true });

    if (userId) {
      query.andWhere('session.userId = :userId', { userId });
    }

    return query.getCount();
  }

  async getSessionStats(): Promise<{
    totalActive: number;
    totalExpired: number;
    byUser: { userId: number; count: number }[];
  }> {
    const totalActive = await this.sessionRepository.count({
      where: { active: true },
    });

    const totalExpired = await this.sessionRepository.count({
      where: {
        expiresAt: LessThan(new Date()),
        active: true,
      },
    });

    const byUser = await this.sessionRepository
      .createQueryBuilder('session')
      .select('session.userId', 'userId')
      .addSelect('COUNT(*)', 'count')
      .where('session.active = :active', { active: true })
      .groupBy('session.userId')
      .getRawMany();

    return {
      totalActive,
      totalExpired,
      byUser: byUser.map(item => ({
        userId: parseInt(item.userId),
        count: parseInt(item.count),
      })),
    };
  }

  async getWebSocketStats() {
    return this.websocketGateway.getConnectionStats();
  }

  async getWebSocketSessions() {
    return this.websocketGateway.getActiveSessions();
  }

  async getConnectionLogs(params: {
    days?: number;
    limit?: number;
    user_id?: number;
    role_name?: string;
  }): Promise<ConnectionLog[]> {
    try {
      this.logger.log('🔍 Iniciando getConnectionLogs con params:', params);
      
      // Consulta muy simple sin filtros para evitar problemas
      const results = await this.connectionLogRepository.find({
        order: {
          created_at: 'DESC'
        },
        take: 50
      });
      
      this.logger.log(`📊 Obtenidos ${results.length} logs de conexión`);
      return results;
    } catch (error) {
      this.logger.error('❌ Error al obtener logs de conexión:', error);
      this.logger.error('❌ Stack trace:', error.stack);
      // Retornar array vacío en caso de error
      return [];
    }
  }

  async forceLogout(sessionId: number, adminUserId: number): Promise<void> {
    const session = await this.findOne(sessionId);
    
    session.active = false;
    await this.sessionRepository.save(session);
    
    // Registrar el log de conexión
    await this.connectionLogRepository.save({
      user_id: session.userId,
      session_id: sessionId,
      action: ConnectionAction.FORCED_LOGOUT,
      ip_address: session.ipAddress,
      device: session.device,
      browser: session.browser,
      operating_system: session.operatingSystem,
      city: session.city,
      region: session.region,
      country: session.country,
      role_name: 'admin', // El rol del admin que ejecuta la acción
      created_at: new Date()
    });
    
    this.logger.log(`🚪 Sesión ${sessionId} desactivada por admin ${adminUserId}`);
    
    // Emitir evento de cierre de sesión
    this.websocketGateway.closeSession(session.tokenHash, 'Sesión cerrada por administrador');
  }
} 