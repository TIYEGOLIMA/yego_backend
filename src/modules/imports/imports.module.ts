import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ImportsController } from './imports.controller';
import { ImportsService } from './imports.service';
import { Import } from './entities/import.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Import])],
  controllers: [ImportsController],
  providers: [ImportsService],
  exports: [ImportsService],
})
export class ImportsModule {} 