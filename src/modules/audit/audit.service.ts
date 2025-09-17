import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between, Like, In } from 'typeorm';
import { AuditLog } from './entities/audit-log.entity';
import { CreateAuditLogDto } from './dto/create-audit-log.dto';
import { Logger } from '@nestjs/common';

@Injectable()
export class AuditService {
  private readonly logger = new Logger(AuditService.name);

  constructor(
    @InjectRepository(AuditLog)
    private auditLogRepository: Repository<AuditLog>,
  ) {}

  async create(createAuditLogDto: CreateAuditLogDto, userId?: number): Promise<AuditLog> {
    const auditLog = this.auditLogRepository.create({
      ...createAuditLogDto,
      userId,
    });

    const savedLog = await this.auditLogRepository.save(auditLog);
    this.logger.log(`📝 Log de auditoría creado: ${savedLog.action} por usuario ${userId || 'sistema'}`);
    
    return savedLog;
  }

  async findAll(
    page: number = 1,
    limit: number = 50,
    filters?: {
      userId?: number;
      action?: string;
      resource?: string;
      startDate?: Date;
      endDate?: Date;
      search?: string;
    },
  ): Promise<{ logs: AuditLog[]; total: number; page: number; limit: number }> {
    const query = this.auditLogRepository.createQueryBuilder('audit')
      .leftJoinAndSelect('audit.user', 'user')
      .orderBy('audit.createdAt', 'DESC');

    // Aplicar filtros
    if (filters?.userId) {
      query.andWhere('audit.userId = :userId', { userId: filters.userId });
    }

    if (filters?.action) {
      query.andWhere('audit.action = :action', { action: filters.action });
    }

    if (filters?.resource) {
      query.andWhere('audit.resource = :resource', { resource: filters.resource });
    }

    if (filters?.startDate && filters?.endDate) {
      query.andWhere('audit.createdAt BETWEEN :startDate AND :endDate', {
        startDate: filters.startDate,
        endDate: filters.endDate,
      });
    }

    if (filters?.search) {
      query.andWhere(
        '(audit.action LIKE :search OR audit.resource LIKE :search OR audit.resourceId LIKE :search)',
        { search: `%${filters.search}%` }
      );
    }

    // Paginación
    const skip = (page - 1) * limit;
    query.skip(skip).take(limit);

    const [logs, total] = await query.getManyAndCount();

    return {
      logs,
      total,
      page,
      limit,
    };
  }

  async findOne(id: number): Promise<AuditLog> {
    return this.auditLogRepository.findOne({
      where: { id },
      relations: ['user'],
    });
  }

  async findByUser(userId: number, limit: number = 100): Promise<AuditLog[]> {
    return this.auditLogRepository.find({
      where: { userId },
      relations: ['user'],
      order: { createdAt: 'DESC' },
      take: limit,
    });
  }

  async findByAction(action: string, limit: number = 100): Promise<AuditLog[]> {
    return this.auditLogRepository.find({
      where: { action },
      relations: ['user'],
      order: { createdAt: 'DESC' },
      take: limit,
    });
  }

  async findByResource(resource: string, limit: number = 100): Promise<AuditLog[]> {
    return this.auditLogRepository.find({
      where: { resource },
      relations: ['user'],
      order: { createdAt: 'DESC' },
      take: limit,
    });
  }

  async getStats(days: number = 30): Promise<{
    totalLogs: number;
    actions: { action: string; count: number }[];
    resources: { resource: string; count: number }[];
    users: { userId: number; username: string; count: number }[];
    dailyStats: { date: string; count: number }[];
  }> {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);

    // Total de logs
    const totalLogs = await this.auditLogRepository.count({
      where: {
        createdAt: Between(startDate, new Date()),
      },
    });

    // Estadísticas por acción
    const actions = await this.auditLogRepository
      .createQueryBuilder('audit')
      .select('audit.action', 'action')
      .addSelect('COUNT(*)', 'count')
      .where('audit.createdAt >= :startDate', { startDate })
      .groupBy('audit.action')
      .orderBy('count', 'DESC')
      .getRawMany();

    // Estadísticas por recurso
    const resources = await this.auditLogRepository
      .createQueryBuilder('audit')
      .select('audit.resource', 'resource')
      .addSelect('COUNT(*)', 'count')
      .where('audit.createdAt >= :startDate', { startDate })
      .andWhere('audit.resource IS NOT NULL')
      .groupBy('audit.resource')
      .orderBy('count', 'DESC')
      .getRawMany();

    // Estadísticas por usuario
    const users = await this.auditLogRepository
      .createQueryBuilder('audit')
      .leftJoin('audit.user', 'user')
      .select('audit.userId', 'userId')
      .addSelect('user.username', 'username')
      .addSelect('COUNT(*)', 'count')
      .where('audit.createdAt >= :startDate', { startDate })
      .andWhere('audit.userId IS NOT NULL')
      .groupBy('audit.userId, user.username')
      .orderBy('count', 'DESC')
      .getRawMany();

    // Estadísticas diarias
    const dailyStats = await this.auditLogRepository
      .createQueryBuilder('audit')
      .select('DATE(audit.createdAt)', 'date')
      .addSelect('COUNT(*)', 'count')
      .where('audit.createdAt >= :startDate', { startDate })
      .groupBy('DATE(audit.createdAt)')
      .orderBy('date', 'ASC')
      .getRawMany();

    return {
      totalLogs,
      actions: actions.map(item => ({
        action: item.action,
        count: parseInt(item.count),
      })),
      resources: resources.map(item => ({
        resource: item.resource,
        count: parseInt(item.count),
      })),
      users: users.map(item => ({
        userId: parseInt(item.userId),
        username: item.username,
        count: parseInt(item.count),
      })),
      dailyStats: dailyStats.map(item => ({
        date: item.date,
        count: parseInt(item.count),
      })),
    };
  }

  async getRecentActivity(limit: number = 20): Promise<AuditLog[]> {
    return this.auditLogRepository.find({
      relations: ['user'],
      order: { createdAt: 'DESC' },
      take: limit,
    });
  }

  async logLogin(userId: number, ipAddress?: string, userAgent?: string, actionType?: string): Promise<void> {
    await this.create({
      action: actionType || 'login',
      resource: 'auth',
      resourceId: userId.toString(),
      details: { success: true, type: actionType || 'normal' },
      ipAddress,
      userAgent,
    }, userId);
  }

  async logLogout(userId: number, ipAddress?: string, userAgent?: string): Promise<void> {
    await this.create({
      action: 'logout',
      resource: 'auth',
      resourceId: userId.toString(),
      details: { success: true },
      ipAddress,
      userAgent,
    }, userId);
  }

  async logFailedLogin(username: string, ipAddress?: string, userAgent?: string): Promise<void> {
    await this.create({
      action: 'login_failed',
      resource: 'auth',
      resourceId: username,
      details: { username, reason: 'Credenciales inválidas' },
      ipAddress,
      userAgent,
    });
  }

  async logUserAction(
    userId: number,
    action: string,
    resource: string,
    resourceId?: string,
    details?: Record<string, any>,
    ipAddress?: string,
    userAgent?: string,
  ): Promise<void> {
    await this.create({
      action,
      resource,
      resourceId,
      details,
      ipAddress,
      userAgent,
    }, userId);
  }

  async logSystemAction(
    action: string,
    resource: string,
    resourceId?: string,
    details?: Record<string, any>,
  ): Promise<void> {
    await this.create({
      action,
      resource,
      resourceId,
      details,
    });
  }
} 