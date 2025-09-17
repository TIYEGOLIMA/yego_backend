import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between, MoreThanOrEqual } from 'typeorm';
import { User } from '../users/entities/user.entity';
import { Role } from '../roles/entities/role.entity';
import { Permission } from '../permissions/entities/permission.entity';
import { Session } from '../sessions/entities/session.entity';
import { AuditLog } from '../audit/entities/audit-log.entity';
import { Import } from '../imports/entities/import.entity';
import { SystemMonitorService } from '../shared/services/system-monitor.service';
import * as ExcelJS from 'exceljs';

@Injectable()
export class ReportsService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    @InjectRepository(Role)
    private readonly roleRepository: Repository<Role>,
    @InjectRepository(Permission)
    private readonly permissionRepository: Repository<Permission>,
    @InjectRepository(Session)
    private readonly sessionRepository: Repository<Session>,
    @InjectRepository(AuditLog)
    private readonly auditLogRepository: Repository<AuditLog>,
    @InjectRepository(Import)
    private readonly importRepository: Repository<Import>,
    private readonly systemMonitorService: SystemMonitorService,
  ) {}

  async getSystemStats(days: number = 30): Promise<any> {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);

    const [
      totalUsers,
      activeUsers,
      totalRoles,
      totalPermissions,
      totalImports,
      activeSessions,
    ] = await Promise.all([
      this.userRepository.count(),
      this.userRepository.count({ where: { active: true } }),
      this.roleRepository.count(),
      this.permissionRepository.count(),
      this.importRepository.count(),
      this.sessionRepository.count({ where: { active: true } }),
    ]);

    return {
      totalUsers,
      activeUsers,
      totalRoles,
      totalPermissions,
      totalImports,
      activeSessions,
    };
  }

  async getDashboardData(): Promise<any> {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayEnd = new Date(today);
    todayEnd.setHours(23, 59, 59, 999);
    
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayEnd = new Date(yesterday);
    yesterdayEnd.setHours(23, 59, 59, 999);
    
    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);

    const [
      systemStats,
      importsToday,
      importsYesterday,
      recentActivity,
      errorCount,
      weeklyStats,
      systemStatus
    ] = await Promise.all([
      this.getSystemStats(30),
      this.importRepository.count({
        where: {
          createdAt: Between(today, todayEnd)
        }
      }),
      this.importRepository.count({
        where: {
          createdAt: Between(yesterday, yesterdayEnd)
        }
      }),
      this.auditLogRepository.find({
        relations: ['user'],
        order: { createdAt: 'DESC' },
        take: 10
      }),
      this.auditLogRepository.count({
        where: {
          action: 'LOGIN_FAILED',
          createdAt: MoreThanOrEqual(weekAgo)
        }
      }),
      this.getWeeklyStats(),
      this.systemMonitorService.getSystemStatus()
    ]);

    // Calcular cambios porcentuales
    const importsChange = importsYesterday > 0 
      ? ((importsToday - importsYesterday) / importsYesterday * 100).toFixed(1)
      : importsToday > 0 ? '100' : '0';

    return {
      metrics: {
        ...systemStats,
        importsToday,
        importsChange: importsChange + '%',
        errorCount
      },
      recentActivity: recentActivity.map(log => ({
        id: log.id,
        user: log.user ? {
          id: log.user.id,
          username: log.user.username,
          name: log.user.name
        } : null,
        action: log.action,
        resource: log.resource,
        resourceId: log.resourceId,
        details: log.details,
        createdAt: log.createdAt,
        ipAddress: log.ipAddress
      })),
      systemStatus: {
        database: systemStatus.database,
        api: systemStatus.api,
        websockets: systemStatus.websockets,
        storage: systemStatus.storage,
        memory: systemStatus.memory,
        cpu: systemStatus.cpu,
        uptime: systemStatus.uptime,
        lastCheck: systemStatus.lastCheck
      },
      weeklyStats
    };
  }

  async getUserStats(): Promise<any[]> {
    const users = await this.userRepository.find({
      order: { createdAt: 'DESC' },
    });

    return users.map(user => ({
      id: user.id,
      name: user.name,
      username: user.username,
      email: user.email,
      role: user.role,
      lastLogin: user.lastLogin,
      active: user.active,
      createdAt: user.createdAt,
    }));
  }

  async getWeeklyStats(): Promise<any> {
    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);

    const [
      newUsers,
      weeklyImports,
      weeklyActivity
    ] = await Promise.all([
      this.userRepository.count({
        where: {
          createdAt: MoreThanOrEqual(weekAgo)
        }
      }),
      this.importRepository.count({
        where: {
          createdAt: MoreThanOrEqual(weekAgo)
        }
      }),
      this.auditLogRepository.count({
        where: {
          createdAt: MoreThanOrEqual(weekAgo)
        }
      })
    ]);

    return {
      newUsers,
      imports: weeklyImports,
      activity: weeklyActivity,
      period: 'Últimos 7 días'
    };
  }

  async exportReport(type: string, days: number = 30): Promise<any> {
    const workbook = new ExcelJS.Workbook();
    const worksheet = workbook.addWorksheet('Reporte');

    switch (type) {
      case 'users':
        await this.exportUsersReport(worksheet);
        break;
      case 'audit':
        await this.exportAuditReport(worksheet, days);
        break;
      case 'sessions':
        await this.exportSessionsReport(worksheet, days);
        break;
      case 'general':
      default:
        await this.exportGeneralReport(worksheet, days);
        break;
    }

    return await workbook.xlsx.writeBuffer();
  }

  private async exportUsersReport(worksheet: ExcelJS.Worksheet) {
    // Configurar columnas
    worksheet.columns = [
      { header: 'ID', key: 'id', width: 10 },
      { header: 'Nombre', key: 'name', width: 30 },
      { header: 'Username', key: 'username', width: 20 },
      { header: 'Email', key: 'email', width: 30 },
      { header: 'Rol', key: 'role', width: 20 },
      { header: 'Estado', key: 'isActive', width: 15 },
      { header: 'Creado', key: 'createdAt', width: 20 },
    ];

    const users = await this.userRepository.find();

    users.forEach(user => {
      worksheet.addRow({
        id: user.id,
        name: user.name,
        username: user.username,
        email: user.email,
        role: user.role,
        isActive: user.active ? 'Activo' : 'Inactivo',
        createdAt: user.createdAt?.toLocaleDateString(),
      });
    });
  }

  private async exportAuditReport(worksheet: ExcelJS.Worksheet, days: number) {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);

    worksheet.columns = [
      { header: 'ID', key: 'id', width: 10 },
      { header: 'Usuario', key: 'user', width: 30 },
      { header: 'Acción', key: 'action', width: 20 },
      { header: 'Recurso', key: 'resource', width: 20 },
      { header: 'Detalles', key: 'details', width: 40 },
      { header: 'IP', key: 'ipAddress', width: 15 },
      { header: 'Fecha', key: 'createdAt', width: 20 },
    ];

    const auditLogs = await this.auditLogRepository
      .createQueryBuilder('auditLog')
      .leftJoinAndSelect('auditLog.user', 'user')
      .where('auditLog.createdAt >= :startDate', { startDate })
      .orderBy('auditLog.createdAt', 'DESC')
      .getMany();

    auditLogs.forEach(log => {
      worksheet.addRow({
        id: log.id,
        user: log.user?.name || 'Sistema',
        action: log.action,
        resource: log.resource,
        details: log.details,
        ipAddress: log.ipAddress,
        createdAt: log.createdAt?.toLocaleString(),
      });
    });
  }

  private async exportSessionsReport(worksheet: ExcelJS.Worksheet, days: number) {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);

    worksheet.columns = [
      { header: 'ID', key: 'id', width: 10 },
      { header: 'Usuario', key: 'user', width: 30 },
      { header: 'IP', key: 'ipAddress', width: 15 },
      { header: 'Dispositivo', key: 'device', width: 30 },
      { header: 'Ciudad', key: 'city', width: 20 },
      { header: 'País', key: 'country', width: 20 },
      { header: 'Login', key: 'loginAt', width: 20 },
      { header: 'Logout', key: 'logoutAt', width: 20 },
      { header: 'Estado', key: 'isActive', width: 15 },
    ];

    const sessions = await this.sessionRepository
      .createQueryBuilder('session')
      .leftJoinAndSelect('session.user', 'user')
      .where('session.createdAt >= :startDate', { startDate })
      .orderBy('session.createdAt', 'DESC')
      .getMany();

    sessions.forEach(session => {
      worksheet.addRow({
        id: session.id,
        user: session.user?.name || 'Desconocido',
        ipAddress: session.ipAddress,
        device: session.device,
        city: session.city,
        country: session.country,
        loginAt: session.createdAt?.toLocaleString(),
        logoutAt: session.active ? 'Activa' : 'Cerrada',
        isActive: session.active ? 'Activa' : 'Cerrada',
      });
    });
  }

  private async exportGeneralReport(worksheet: ExcelJS.Worksheet, days: number) {
    const stats = await this.getSystemStats(days);
    
    worksheet.columns = [
      { header: 'Métrica', key: 'metric', width: 30 },
      { header: 'Valor', key: 'value', width: 20 },
    ];

    worksheet.addRow({ metric: 'Usuarios Totales', value: stats.totalUsers });
    worksheet.addRow({ metric: 'Usuarios Activos', value: stats.activeUsers });
    worksheet.addRow({ metric: 'Roles', value: stats.totalRoles });
    worksheet.addRow({ metric: 'Permisos', value: stats.totalPermissions });
    worksheet.addRow({ metric: 'Importaciones', value: stats.totalImports });
    worksheet.addRow({ metric: 'Sesiones Activas', value: stats.activeSessions });
    worksheet.addRow({ metric: 'Período (días)', value: days });
    worksheet.addRow({ metric: 'Fecha de Reporte', value: new Date().toLocaleString() });
  }
} 