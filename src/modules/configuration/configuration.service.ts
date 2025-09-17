import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Configuration } from './entities/configuration.entity';
import { UpdateConfigurationDto } from './dto/update-configuration.dto';
import { Logger } from '@nestjs/common';

@Injectable()
export class ConfigurationService {
  private readonly logger = new Logger(ConfigurationService.name);
  private cache = new Map<string, any>();

  constructor(
    @InjectRepository(Configuration)
    private configurationRepository: Repository<Configuration>,
  ) {}

  async findAll(): Promise<Configuration[]> {
    return this.configurationRepository.find({
      order: { category: 'ASC', key: 'ASC' },
    });
  }

  async findByCategory(category: string): Promise<Configuration[]> {
    return this.configurationRepository.find({
      where: { category },
      order: { key: 'ASC' },
    });
  }

  async findOne(key: string): Promise<Configuration> {
    const config = await this.configurationRepository.findOne({
      where: { key },
    });

    if (!config) {
      throw new NotFoundException(`Configuración con clave '${key}' no encontrada`);
    }

    return config;
  }

  async getValue(key: string, defaultValue?: any): Promise<any> {
    // Verificar cache primero
    if (this.cache.has(key)) {
      return this.cache.get(key);
    }

    try {
      const config = await this.findOne(key);
      const value = this.parseValue(config.value, config.type);
      
      // Guardar en cache
      this.cache.set(key, value);
      
      return value;
    } catch (error) {
      if (defaultValue !== undefined) {
        return defaultValue;
      }
      throw error;
    }
  }

  async setValue(key: string, value: any, type: string = 'string'): Promise<Configuration> {
    let config = await this.configurationRepository.findOne({
      where: { key },
    });

    if (!config) {
      config = this.configurationRepository.create({
        key,
        value: this.stringifyValue(value, type),
        type,
      });
    } else {
      config.value = this.stringifyValue(value, type);
      config.type = type;
    }

    const savedConfig = await this.configurationRepository.save(config);
    
    // Actualizar cache
    this.cache.set(key, value);
    
    this.logger.log(`⚙️ Configuración actualizada: ${key} = ${value}`);
    
    return savedConfig;
  }

  async update(key: string, updateConfigurationDto: UpdateConfigurationDto): Promise<Configuration> {
    const config = await this.findOne(key);
    
    Object.assign(config, updateConfigurationDto);
    
    const savedConfig = await this.configurationRepository.save(config);
    
    // Limpiar cache para esta clave
    this.cache.delete(key);
    
    this.logger.log(`⚙️ Configuración actualizada: ${key}`);
    
    return savedConfig;
  }

  async remove(key: string): Promise<void> {
    const config = await this.findOne(key);
    await this.configurationRepository.remove(config);
    
    // Limpiar cache
    this.cache.delete(key);
    
    this.logger.log(`🗑️ Configuración eliminada: ${key}`);
  }

  async getCategories(): Promise<string[]> {
    const categories = await this.configurationRepository
      .createQueryBuilder('config')
      .select('DISTINCT config.category', 'category')
      .where('config.category IS NOT NULL')
      .getRawMany();

    return categories.map(item => item.category);
  }

  async getSystemConfig(): Promise<{
    system: Record<string, any>;
    security: Record<string, any>;
    ui: Record<string, any>;
    audit: Record<string, any>;
    imports: Record<string, any>;
  }> {
    const configs = await this.findAll();
    
    const result = {
      system: {},
      security: {},
      ui: {},
      audit: {},
      imports: {},
    };

    for (const config of configs) {
      const value = this.parseValue(config.value, config.type);
      
      if (config.category && result[config.category]) {
        result[config.category][config.key] = value;
      } else {
        result.system[config.key] = value;
      }
    }

    return result;
  }

  async initializeDefaultConfigs(): Promise<void> {
    const defaultConfigs = [
      {
        key: 'system_name',
        value: 'Yego Integral',
        description: 'Nombre del sistema',
        category: 'system',
        type: 'string',
      },
      {
        key: 'system_version',
        value: '1.0.0',
        description: 'Versión del sistema',
        category: 'system',
        type: 'string',
      },
      {
        key: 'session_timeout',
        value: '3600',
        description: 'Tiempo de sesión en segundos',
        category: 'security',
        type: 'number',
      },
      {
        key: 'max_login_attempts',
        value: '5',
        description: 'Máximo intentos de login',
        category: 'security',
        type: 'number',
      },
      {
        key: 'password_min_length',
        value: '8',
        description: 'Longitud mínima de contraseña',
        category: 'security',
        type: 'number',
      },
      {
        key: 'enable_audit_logs',
        value: 'true',
        description: 'Habilitar logs de auditoría',
        category: 'audit',
        type: 'boolean',
      },
      {
        key: 'import_max_file_size',
        value: '10485760',
        description: 'Tamaño máximo de archivo de importación (10MB)',
        category: 'imports',
        type: 'number',
      },
      {
        key: 'default_language',
        value: 'es',
        description: 'Idioma por defecto',
        category: 'system',
        type: 'string',
      },
      {
        key: 'theme_default',
        value: 'light',
        description: 'Tema por defecto',
        category: 'ui',
        type: 'string',
      },
    ];

    for (const defaultConfig of defaultConfigs) {
      const existing = await this.configurationRepository.findOne({
        where: { key: defaultConfig.key },
      });

      if (!existing) {
        await this.configurationRepository.save(defaultConfig);
        this.logger.log(`⚙️ Configuración por defecto creada: ${defaultConfig.key}`);
      }
    }
  }

  private parseValue(value: string, type: string): any {
    if (!value) return null;

    switch (type) {
      case 'number':
        return parseFloat(value);
      case 'boolean':
        return value.toLowerCase() === 'true';
      case 'json':
        try {
          return JSON.parse(value);
        } catch {
          return value;
        }
      default:
        return value;
    }
  }

  private stringifyValue(value: any, type: string): string {
    if (value === null || value === undefined) return '';

    switch (type) {
      case 'json':
        return JSON.stringify(value);
      default:
        return String(value);
    }
  }

  clearCache(): void {
    this.cache.clear();
    this.logger.log('🧹 Cache de configuraciones limpiado');
  }
}
 