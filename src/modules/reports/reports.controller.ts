import { Controller, Get, Query, Res, UseGuards } from '@nestjs/common';
import { Response } from 'express';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { ReportsService } from './reports.service';

@Controller('reports')
@UseGuards(JwtAuthGuard, RolesGuard)
export class ReportsController {
  constructor(private readonly reportsService: ReportsService) {}

  @Get('stats')
  @Roles('superadmin', 'admin')
  async getSystemStats(@Query('days') days: string = '30') {
    return this.reportsService.getSystemStats(parseInt(days));
  }

  @Get('dashboard')
  @Roles('superadmin', 'admin', 'supervisor')
  async getDashboardData() {
    return this.reportsService.getDashboardData();
  }

  @Get('users')
  @Roles('superadmin', 'admin')
  async getUserStats() {
    return this.reportsService.getUserStats();
  }

  @Get('export/:type')
  @Roles('superadmin', 'admin')
  async exportReport(
    @Query('type') type: string,
    @Query('days') days: string = '30',
    @Res() res: Response
  ) {
    const buffer = await this.reportsService.exportReport(type, parseInt(days));
    
    res.set({
      'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'Content-Disposition': `attachment; filename=reporte-${type}-${new Date().toISOString().split('T')[0]}.xlsx`,
      'Content-Length': buffer.length,
    });
    
    res.end(buffer);
  }
} 