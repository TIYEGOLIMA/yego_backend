import { Controller, Get, Post, Body, Param, Delete, UseGuards, UseInterceptors, UploadedFile, Query } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiConsumes, ApiBody, ApiQuery } from '@nestjs/swagger';
import { ImportsService } from './imports.service';
import { UploadImportDto } from './dto/upload-import.dto';
import { ProcessImportDto } from './dto/process-import.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { diskStorage } from 'multer';
import { extname } from 'path';

@ApiTags('Imports')
@Controller('imports')
@UseGuards(JwtAuthGuard, RolesGuard)
@ApiBearerAuth()
export class ImportsController {
  constructor(private readonly importsService: ImportsService) {}

  @Post('upload')
  @ApiOperation({ summary: 'Subir archivo CSV para importación' })
  @ApiResponse({ status: 201, description: 'Archivo subido exitosamente' })
  @ApiResponse({ status: 400, description: 'Archivo inválido' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        file: {
          type: 'string',
          format: 'binary',
          description: 'Archivo CSV',
        },
        type: {
          type: 'string',
          enum: ['users', 'roles', 'permissions'],
          description: 'Tipo de importación',
        },
      },
    },
  })
  @UseInterceptors(
    FileInterceptor('file', {
      storage: diskStorage({
        destination: './uploads/imports',
        filename: (req, file, cb) => {
          const randomName = Array(32)
            .fill(null)
            .map(() => Math.round(Math.random() * 16).toString(16))
            .join('');
          return cb(null, `${randomName}${extname(file.originalname)}`);
        },
      }),
      fileFilter: (req, file, cb) => {
        if (file.mimetype !== 'text/csv') {
          return cb(new Error('Solo se permiten archivos CSV'), false);
        }
        cb(null, true);
      },
    }),
  )
  @Roles('admin', 'supervisor')
  async uploadFile(
    @UploadedFile() file: Express.Multer.File,
    @Body('type') type: string,
    @Body('userId') userId: number,
  ) {
    const uploadImportDto: UploadImportDto = {
      type: type as 'users' | 'roles' | 'permissions',
      filename: file.originalname,
    };

    const importRecord = await this.importsService.create(userId, uploadImportDto);

    // Procesar el archivo para obtener vista previa
    const result = await this.importsService.processCsvFile(file.path, type);
    
    // Actualizar la importación con la vista previa
    await this.importsService.updatePreview(
      importRecord.id,
      result.preview,
      result.errors,
      result.totalRows,
    );

    return {
      message: 'Archivo subido y procesado exitosamente',
      importId: importRecord.id,
      preview: result.preview,
      errors: result.errors,
      totalRows: result.totalRows,
    };
  }

  @Get()
  @ApiOperation({ summary: 'Obtener todas las importaciones' })
  @ApiResponse({ status: 200, description: 'Lista de importaciones obtenida exitosamente' })
  @ApiQuery({ name: 'userId', required: false, description: 'Filtrar por usuario' })
  @ApiQuery({ name: 'startDate', required: false, description: 'Fecha de inicio (YYYY-MM-DD)' })
  @ApiQuery({ name: 'endDate', required: false, description: 'Fecha de fin (YYYY-MM-DD)' })
  @Roles('admin', 'supervisor')
  async findAll(
    @Query('userId') userId?: number,
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string
  ) {
    return this.importsService.findAll(userId, startDate, endDate);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Obtener importación por ID' })
  @ApiResponse({ status: 200, description: 'Importación encontrada exitosamente' })
  @ApiResponse({ status: 404, description: 'Importación no encontrada' })
  @Roles('admin', 'supervisor')
  async findOne(@Param('id') id: string) {
    return this.importsService.findOne(+id);
  }

  @Get(':id/preview')
  @ApiOperation({ summary: 'Obtener vista previa de importación' })
  @ApiResponse({ status: 200, description: 'Vista previa obtenida exitosamente' })
  @ApiResponse({ status: 404, description: 'Importación no encontrada' })
  @Roles('admin', 'supervisor')
  async getPreview(@Param('id') id: string) {
    return this.importsService.getPreview(+id);
  }

  @Post(':id/process')
  @ApiOperation({ summary: 'Procesar importación' })
  @ApiResponse({ status: 200, description: 'Importación procesada exitosamente' })
  @ApiResponse({ status: 404, description: 'Importación no encontrada' })
  @Roles('admin', 'supervisor')
  async processImport(
    @Param('id') id: string,
    @Body() processImportDto: ProcessImportDto,
  ) {
    return this.importsService.processImport(+id, processImportDto);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Eliminar importación' })
  @ApiResponse({ status: 200, description: 'Importación eliminada exitosamente' })
  @ApiResponse({ status: 404, description: 'Importación no encontrada' })
  @Roles('admin')
  async remove(@Param('id') id: string) {
    return this.importsService.remove(+id);
  }
} 