import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { getDatabaseConfig } from './config/database.config';
import { AuthModule } from './modules/auth/auth.module';
import { UsersModule } from './modules/users/users.module';
import { RolesModule } from './modules/roles/roles.module';
import { SessionsModule } from './modules/sessions/sessions.module';
import { AuditModule } from './modules/audit/audit.module';
import { ConfigurationModule } from './modules/configuration/configuration.module';
import { WebsocketModule } from './modules/websocket/websocket.module';
import { SharedModule } from './modules/shared/shared.module';
import { PermissionsModule } from './modules/permissions/permissions.module';
import { ImportsModule } from './modules/imports/imports.module';
import { ModulesModule } from './modules/modules/modules.module';
import { ReportsModule } from './modules/reports/reports.module';
import { TicketsModule } from './modules/tickets/tickets.module';
import { User } from './modules/users/entities/user.entity';
import { Role } from './modules/roles/entities/role.entity';
import { Session } from './modules/sessions/entities/session.entity';
import { ConnectionLog } from './modules/sessions/entities/connection-log.entity';
import { AuditLog } from './modules/audit/entities/audit-log.entity';
import { Configuration } from './modules/configuration/entities/configuration.entity';
import { Permission } from './modules/permissions/entities/permission.entity';
import { Import } from './modules/imports/entities/import.entity';
import { ModuleEntity } from './modules/modules/entities/module.entity';
import { Ticket } from './modules/tickets/entities/ticket.entity';

@Module({
  imports: [
    // Environment configuration
    ConfigModule.forRoot({
      envFilePath: process.env.NODE_ENV === 'production' ? '.env.production' : '.env.development',
      isGlobal: true,
    }),    
    
    // Database configuration
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (configService: ConfigService) => ({
        ...getDatabaseConfig(configService),
        entities: [
          User,
          Role,
          Session,
          ConnectionLog,
          AuditLog,
          Configuration,
          Permission,
          Import,
          ModuleEntity,
          Ticket,
        ],
      }),
      inject: [ConfigService],
    }),
    

    // Application modules
    SharedModule,
    AuthModule,
    UsersModule,
    RolesModule,
    SessionsModule,
    AuditModule,
    ConfigurationModule,
    WebsocketModule,
    PermissionsModule,
    ImportsModule,
    ModulesModule,
    ReportsModule,
    TicketsModule,
  ],
  controllers: [],
  providers: [],
})
export class AppModule {} 