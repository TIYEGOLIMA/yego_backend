import { Injectable, Logger, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Import } from './entities/import.entity';
import { UploadImportDto } from './dto/upload-import.dto';
import { ProcessImportDto } from './dto/process-import.dto';
import csv from 'csv-parser';
import * as fs from 'fs';

@Injectable()
export class ImportsService {
  private readonly logger = new Logger(ImportsService.name);

  constructor(
    @InjectRepository(Import)
    private importRepository: Repository<Import>,
  ) {}

  async create(userId: number, uploadImportDto: UploadImportDto): Promise<Import> {
    const importRecord = this.importRepository.create({
      userId,
      filename: uploadImportDto.filename,
      type: uploadImportDto.type,
      status: 'pending',
    });

    const savedImport = await this.importRepository.save(importRecord);
    
    this.logger.log(`✅ Importación creada: ${savedImport.filename}`);
    
    return savedImport;
  }

  async findAll(userId?: number, startDate?: string, endDate?: string): Promise<Import[]> {
    const queryBuilder = this.importRepository.createQueryBuilder('import')
      .leftJoinAndSelect('import.user', 'user')
      .orderBy('import.createdAt', 'DESC');

    if (userId) {
      queryBuilder.andWhere('import.userId = :userId', { userId });
    }

    if (startDate && endDate) {
      queryBuilder.andWhere('DATE(import.createdAt) BETWEEN :startDate AND :endDate', {
        startDate,
        endDate,
      });
    } else if (startDate) {
      queryBuilder.andWhere('DATE(import.createdAt) >= :startDate', { startDate });
    } else if (endDate) {
      queryBuilder.andWhere('DATE(import.createdAt) <= :endDate', { endDate });
    }

    return queryBuilder.getMany();
  }

  async findOne(id: number): Promise<Import> {
    const importRecord = await this.importRepository.findOne({
      where: { id },
      relations: ['user'],
    });

    if (!importRecord) {
      throw new NotFoundException(`Importación con ID ${id} no encontrada`);
    }

    return importRecord;
  }

  async getPreview(id: number): Promise<any> {
    const importRecord = await this.findOne(id);
    
    if (!importRecord.preview) {
      throw new BadRequestException('No hay vista previa disponible para esta importación');
    }

    return {
      id: importRecord.id,
      filename: importRecord.filename,
      type: importRecord.type,
      totalRows: importRecord.totalRows,
      preview: importRecord.preview,
      errors: importRecord.errors,
    };
  }

  async processCsvFile(filePath: string, type: string): Promise<{
    preview: any[];
    errors: Record<string, any>;
    totalRows: number;
  }> {
    const results: any[] = [];
    const errors: Record<string, any> = {};
    let rowNumber = 0;

    return new Promise((resolve, reject) => {
      fs.createReadStream(filePath)
        .pipe(csv())
        .on('data', (data) => {
          rowNumber++;
          
          try {
            const validatedRow = this.validateRow(data, type, rowNumber);
            if (validatedRow.isValid) {
              results.push({
                id: rowNumber,
                ...validatedRow.data,
              });
            } else {
              errors[rowNumber] = validatedRow.errors;
            }
          } catch (error) {
            errors[rowNumber] = ['Error de validación: ' + error.message];
          }
        })
        .on('end', () => {
          resolve({
            preview: results.slice(0, 10), // Solo las primeras 10 filas para preview
            errors,
            totalRows: rowNumber,
          });
        })
        .on('error', (error) => {
          reject(error);
        });
    });
  }

  private validateRow(data: any, type: string, rowNumber: number): {
    isValid: boolean;
    data?: any;
    errors?: string[];
  } {
    const errors: string[] = [];

    switch (type) {
      case 'users':
        return this.validateUserRow(data, rowNumber);
      case 'roles':
        return this.validateRoleRow(data, rowNumber);
      case 'permissions':
        return this.validatePermissionRow(data, rowNumber);
      default:
        errors.push('Tipo de importación no válido');
        return { isValid: false, errors };
    }
  }

  private validateUserRow(data: any, rowNumber: number): {
    isValid: boolean;
    data?: any;
    errors?: string[];
  } {
    const errors: string[] = [];

    // Validar campos requeridos según documentación: first_name, last_name, email, national_id, role
    if (!data.first_name || data.first_name.trim() === '') {
      errors.push('first_name es requerido');
    }

    if (!data.last_name || data.last_name.trim() === '') {
      errors.push('last_name es requerido');
    }

    if (!data.email || !this.isValidEmail(data.email)) {
      errors.push('email es requerido y debe ser válido');
    }

    if (!data.national_id || data.national_id.trim() === '') {
      errors.push('national_id es requerido');
    }

    if (!data.role || data.role.trim() === '') {
      errors.push('role es requerido');
    }

    if (errors.length > 0) {
      return { isValid: false, errors };
    }

    return {
      isValid: true,
      data: {
        firstName: data.first_name.trim(),
        lastName: data.last_name.trim(),
        email: data.email.trim().toLowerCase(),
        dni: data.national_id.trim(),
        role: data.role.trim(),
      },
    };
  }

  private validateRoleRow(data: any, rowNumber: number): {
    isValid: boolean;
    data?: any;
    errors?: string[];
  } {
    const errors: string[] = [];

    if (!data.name || data.name.trim() === '') {
      errors.push('name es requerido');
    }

    if (!data.description || data.description.trim() === '') {
      errors.push('description es requerido');
    }

    if (errors.length > 0) {
      return { isValid: false, errors };
    }

    return {
      isValid: true,
      data: {
        name: data.name.trim(),
        description: data.description.trim(),
      },
    };
  }

  private validatePermissionRow(data: any, rowNumber: number): {
    isValid: boolean;
    data?: any;
    errors?: string[];
  } {
    const errors: string[] = [];

    if (!data.name || data.name.trim() === '') {
      errors.push('name es requerido');
    }

    if (!data.module || data.module.trim() === '') {
      errors.push('module es requerido');
    }

    if (!data.action || data.action.trim() === '') {
      errors.push('action es requerido');
    }

    if (errors.length > 0) {
      return { isValid: false, errors };
    }

    return {
      isValid: true,
      data: {
        name: data.name.trim(),
        module: data.module.trim(),
        action: data.action.trim(),
        description: data.description?.trim() || '',
      },
    };
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  async updatePreview(id: number, preview: any[], errors: Record<string, any>, totalRows: number): Promise<Import> {
    const importRecord = await this.findOne(id);
    
    importRecord.preview = preview;
    importRecord.errors = errors;
    importRecord.totalRows = totalRows;
    importRecord.status = 'pending';

    const savedImport = await this.importRepository.save(importRecord);
    
    this.logger.log(`✅ Vista previa actualizada para importación: ${savedImport.filename}`);
    
    return savedImport;
  }

  async processImport(id: number, processImportDto: ProcessImportDto): Promise<Import> {
    const importRecord = await this.findOne(id);
    
    if (importRecord.status !== 'pending') {
      throw new BadRequestException('La importación ya fue procesada');
    }

    importRecord.status = 'processing';
    await this.importRepository.save(importRecord);

    try {
      // Aquí se implementaría la lógica de procesamiento real
      // Por ahora simulamos el procesamiento
      await this.simulateProcessing(importRecord, processImportDto);
      
      importRecord.status = 'completed';
      importRecord.processedRows = importRecord.totalRows;
      importRecord.successRows = importRecord.totalRows - Object.keys(importRecord.errors || {}).length;
      importRecord.errorRows = Object.keys(importRecord.errors || {}).length;
      
      const savedImport = await this.importRepository.save(importRecord);
      
      this.logger.log(`✅ Importación procesada: ${savedImport.filename}`);
      
      return savedImport;
    } catch (error) {
      importRecord.status = 'failed';
      await this.importRepository.save(importRecord);
      
      this.logger.error(`❌ Error procesando importación: ${error.message}`);
      throw error;
    }
  }

  private async simulateProcessing(importRecord: Import, processImportDto: ProcessImportDto): Promise<void> {
    // Simular procesamiento
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    // Aquí se implementaría la lógica real de importación
    // Por ejemplo, crear usuarios, roles, permisos, etc.
  }

  async remove(id: number): Promise<void> {
    const importRecord = await this.findOne(id);
    await this.importRepository.remove(importRecord);
    
    this.logger.log(`✅ Importación eliminada: ${importRecord.filename}`);
  }
} 