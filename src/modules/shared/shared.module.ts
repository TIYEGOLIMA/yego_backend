import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { GeoService } from './services/geo.service';
import { DeviceService } from './services/device.service';
import { DniValidationService } from './services/dni-validation.service';
import { SystemMonitorService } from './services/system-monitor.service';
import { GeoController } from './geo.controller';
import { Session } from '../sessions/entities/session.entity';
import { AuditLog } from '../audit/entities/audit-log.entity';
import { Import } from '../imports/entities/import.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([Session, AuditLog, Import])
  ],
  providers: [GeoService, DeviceService, DniValidationService, SystemMonitorService],
  controllers: [GeoController],
  exports: [GeoService, DeviceService, DniValidationService, SystemMonitorService],
})
export class SharedModule {} 