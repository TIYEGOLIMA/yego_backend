import { ConfigService } from '@nestjs/config';
import { TypeOrmModuleOptions } from '@nestjs/typeorm';
import { User } from '../modules/users/entities/user.entity';
import { Role } from '../modules/roles/entities/role.entity';
import { Permission } from '../modules/permissions/entities/permission.entity';
import { ModuleEntity } from '../modules/modules/entities/module.entity';
import { Import } from '../modules/imports/entities/import.entity';
import { Session } from '../modules/sessions/entities/session.entity';
import { ConnectionLog } from '../modules/sessions/entities/connection-log.entity';
import { AuditLog } from '../modules/audit/entities/audit-log.entity';
import { Configuration } from '../modules/configuration/entities/configuration.entity';

export const getDatabaseConfig = (configService: ConfigService): TypeOrmModuleOptions => ({
  type: 'postgres',
  host: configService.get('DB_HOST'),
  port: configService.get('DB_PORT'),
  username: configService.get('DB_USER'),
  password: configService.get('DB_PASSWORD'),
  database: configService.get('DB_NAME'),
  entities: [
    User, 
    Role, 
    Permission, 
    ModuleEntity, 
    Import, 
    Session, 
    ConnectionLog, 
    AuditLog, 
    Configuration
  ],
  synchronize: false, // Deshabilitado para manejar migraciones manualmente
  logging: configService.get('TYPEORM_LOGGING') === 'true',
  ssl: configService.get('NODE_ENV') === 'production' ? { rejectUnauthorized: false } : false,
}); 