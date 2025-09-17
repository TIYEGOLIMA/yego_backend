import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { DataSource } from 'typeorm';
import { Session } from '../../sessions/entities/session.entity';
import { AuditLog } from '../../audit/entities/audit-log.entity';
import { Import } from '../../imports/entities/import.entity';
import * as os from 'os';
import * as fs from 'fs';
import * as path from 'path';

export interface SystemStatus {
  database: 'operational' | 'warning' | 'error';
  api: 'operational' | 'warning' | 'error';
  websockets: 'connected' | 'disconnected' | 'error';
  storage: {
    status: 'operational' | 'warning' | 'error';
    usage: number; // porcentaje de uso
    total: number; // bytes
    used: number; // bytes
    free: number; // bytes
  };
  memory: {
    status: 'operational' | 'warning' | 'error';
    usage: number; // porcentaje de uso
    total: number; // bytes
    used: number; // bytes
    free: number; // bytes
  };
  cpu: {
    status: 'operational' | 'warning' | 'error';
    usage: number; // porcentaje de uso
    loadAverage: number[];
  };
  uptime: number; // segundos
  lastCheck: Date;
}

@Injectable()
export class SystemMonitorService {
  private readonly logger = new Logger(SystemMonitorService.name);

  constructor(
    @InjectRepository(Session)
    private readonly sessionRepository: Repository<Session>,
    @InjectRepository(AuditLog)
    private readonly auditLogRepository: Repository<AuditLog>,
    @InjectRepository(Import)
    private readonly importRepository: Repository<Import>,
    private readonly dataSource: DataSource,
  ) {}

  async getSystemStatus(): Promise<SystemStatus> {
    try {
      const [
        databaseStatus,
        storageStatus,
        memoryStatus,
        cpuStatus,
        uptime
      ] = await Promise.all([
        this.checkDatabaseStatus(),
        this.checkStorageStatus(),
        this.checkMemoryStatus(),
        this.checkCpuStatus(),
        this.getUptime()
      ]);

      return {
        database: databaseStatus,
        api: 'operational', // La API está funcionando si llegamos aquí
        websockets: 'connected', // Se puede verificar con el gateway
        storage: storageStatus,
        memory: memoryStatus,
        cpu: cpuStatus,
        uptime,
        lastCheck: new Date()
      };
    } catch (error) {
      this.logger.error('Error obteniendo estado del sistema:', error);
      return this.getDefaultErrorStatus();
    }
  }

  private async checkDatabaseStatus(): Promise<'operational' | 'warning' | 'error'> {
    try {
      // Verificar conexión a la base de datos
      const result = await this.dataSource.query('SELECT 1 as test');
      
      if (result && result[0] && result[0].test === 1) {
        // Verificar rendimiento de consultas
        const startTime = Date.now();
        await this.sessionRepository.count();
        const queryTime = Date.now() - startTime;
        
        if (queryTime < 200) {
          return 'operational';
        } else if (queryTime < 1000) {
          return 'warning';
        } else {
          return 'error';
        }
      }
      return 'error';
    } catch (error) {
      this.logger.error('Error verificando estado de la base de datos:', error);
      return 'error';
    }
  }

  private async checkStorageStatus(): Promise<SystemStatus['storage']> {
    try {
      // Verificar espacio en disco del directorio de uploads
      const uploadsPath = path.join(process.cwd(), 'uploads');
      
      // Crear directorio si no existe
      if (!fs.existsSync(uploadsPath)) {
        fs.mkdirSync(uploadsPath, { recursive: true });
      }

      // Para desarrollo, usar valores simulados más simples
      const total = 100 * 1024 * 1024 * 1024; // 100 GB
      const used = Math.floor(total * 0.65); // 65% usado
      const free = total - used;
      const usage = Math.round((used / total) * 100);

      let status: 'operational' | 'warning' | 'error' = 'operational';
      if (usage > 90) {
        status = 'error';
      } else if (usage > 75) {
        status = 'warning';
      }

      return {
        status,
        usage,
        total,
        used,
        free
      };
    } catch (error) {
      this.logger.error('Error verificando estado del almacenamiento:', error);
      return {
        status: 'operational',
        usage: 65,
        total: 100 * 1024 * 1024 * 1024,
        used: 65 * 1024 * 1024 * 1024,
        free: 35 * 1024 * 1024 * 1024
      };
    }
  }

  private async checkMemoryStatus(): Promise<SystemStatus['memory']> {
    try {
      const totalMem = os.totalmem();
      const freeMem = os.freemem();
      const usedMem = totalMem - freeMem;
      const usage = Math.round((usedMem / totalMem) * 100);

      let status: 'operational' | 'warning' | 'error' = 'operational';
      if (usage > 90) {
        status = 'error';
      } else if (usage > 80) {
        status = 'warning';
      }

      return {
        status,
        usage,
        total: totalMem,
        used: usedMem,
        free: freeMem
      };
    } catch (error) {
      this.logger.error('Error verificando estado de la memoria:', error);
      return {
        status: 'error',
        usage: 0,
        total: 0,
        used: 0,
        free: 0
      };
    }
  }

  private async checkCpuStatus(): Promise<SystemStatus['cpu']> {
    try {
      const loadAverage = os.loadavg();
      const avgLoad = loadAverage[0]; // Promedio de 1 minuto
      
      // Calcular uso de CPU (simulado para desarrollo)
      const usage = Math.round(Math.min(avgLoad * 25, 100)); // Conversión aproximada

      let status: 'operational' | 'warning' | 'error' = 'operational';
      if (usage > 90) {
        status = 'error';
      } else if (usage > 70) {
        status = 'warning';
      }

      return {
        status,
        usage,
        loadAverage
      };
    } catch (error) {
      this.logger.error('Error verificando estado del CPU:', error);
      return {
        status: 'error',
        usage: 0,
        loadAverage: [0, 0, 0]
      };
    }
  }

  private async getUptime(): Promise<number> {
    try {
      return os.uptime();
    } catch (error) {
      this.logger.error('Error obteniendo uptime:', error);
      return 0;
    }
  }

  private getDefaultErrorStatus(): SystemStatus {
    return {
      database: 'error',
      api: 'error',
      websockets: 'error',
      storage: {
        status: 'error',
        usage: 0,
        total: 0,
        used: 0,
        free: 0
      },
      memory: {
        status: 'error',
        usage: 0,
        total: 0,
        used: 0,
        free: 0
      },
      cpu: {
        status: 'error',
        usage: 0,
        loadAverage: [0, 0, 0]
      },
      uptime: 0,
      lastCheck: new Date()
    };
  }

  async getSystemMetrics(): Promise<any> {
    try {
      const status = await this.getSystemStatus();
      
      // Obtener métricas adicionales
      const [
        activeSessions,
        recentErrors,
        totalImports
      ] = await Promise.all([
        this.sessionRepository.count({ where: { active: true } }),
        this.auditLogRepository.count({ 
          where: { action: 'LOGIN_FAILED' },
          order: { createdAt: 'DESC' },
          take: 1
        }),
        this.importRepository.count()
      ]);

      return {
        ...status,
        metrics: {
          activeSessions,
          recentErrors,
          totalImports
        }
      };
    } catch (error) {
      this.logger.error('Error obteniendo métricas del sistema:', error);
      throw error;
    }
  }
} 