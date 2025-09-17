import { Injectable, Logger, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ModuleEntity } from './entities/module.entity';
import { CreateModuleDto } from './dto/create-module.dto';
import { UpdateModuleDto } from './dto/update-module.dto';

@Injectable()
export class ModulesService {
  private readonly logger = new Logger(ModulesService.name);

  constructor(
    @InjectRepository(ModuleEntity)
    private moduleRepository: Repository<ModuleEntity>,
  ) {}

  async create(createModuleDto: CreateModuleDto): Promise<ModuleEntity> {
    // Verificar si el módulo ya existe
    const existingModule = await this.moduleRepository.findOne({
      where: { name: createModuleDto.name },
    });

    if (existingModule) {
      throw new ConflictException(`El módulo '${createModuleDto.name}' ya existe`);
    }

    const module = this.moduleRepository.create(createModuleDto);
    const savedModule = await this.moduleRepository.save(module);
    
    this.logger.log(`✅ Módulo creado: ${savedModule.name}`);
    
    return savedModule;
  }

  async findAll(): Promise<ModuleEntity[]> {
    return this.moduleRepository.find({
      where: { active: true },
      order: { order: 'ASC', name: 'ASC' },
    });
  }

  async findAllIncludingInactive(): Promise<ModuleEntity[]> {
    return this.moduleRepository.find({
      order: { order: 'ASC', name: 'ASC' },
    });
  }

  async findActive(): Promise<ModuleEntity[]> {
    return this.moduleRepository.find({
      where: { active: true },
      order: { order: 'ASC', name: 'ASC' },
    });
  }

  async findOne(id: number): Promise<ModuleEntity> {
    const module = await this.moduleRepository.findOne({
      where: { id },
    });

    if (!module) {
      throw new NotFoundException(`Módulo con ID ${id} no encontrado`);
    }

    return module;
  }

  async findByName(name: string): Promise<ModuleEntity | null> {
    return this.moduleRepository.findOne({
      where: { name },
    });
  }

  async update(id: number, updateModuleDto: UpdateModuleDto): Promise<ModuleEntity> {
    const module = await this.findOne(id);

    // Verificar si el nuevo nombre ya existe (si se está cambiando)
    if (updateModuleDto.name && updateModuleDto.name !== module.name) {
      const existingModule = await this.findByName(updateModuleDto.name);
      if (existingModule) {
        throw new ConflictException(`El módulo '${updateModuleDto.name}' ya existe`);
      }
    }

    Object.assign(module, updateModuleDto);
    const savedModule = await this.moduleRepository.save(module);
    
    this.logger.log(`✅ Módulo actualizado: ${savedModule.name}`);
    
    return savedModule;
  }

  async remove(id: number): Promise<void> {
    const module = await this.findOne(id);
    
    // Soft delete
    module.active = false;
    await this.moduleRepository.save(module);
    
    this.logger.log(`✅ Módulo desactivado: ${module.name}`);
  }

  async initializeDefaultModules(): Promise<void> {
    const defaultModules = [
      {
        name: 'users',
        description: 'Gestión de Usuarios',
        route: '/users',
        icon: 'Users',
        order: 1,
        permissions: ['users.read', 'users.create', 'users.update', 'users.delete'],
      },
      {
        name: 'roles',
        description: 'Gestión de Roles',
        route: '/roles',
        icon: 'Shield',
        order: 2,
        permissions: ['roles.read', 'roles.create', 'roles.update', 'roles.delete'],
      },
      {
        name: 'permissions',
        description: 'Gestión de Permisos',
        route: '/permissions',
        icon: 'Key',
        order: 3,
        permissions: ['permissions.read', 'permissions.create', 'permissions.update', 'permissions.delete'],
      },
      {
        name: 'modules',
        description: 'Gestión de Módulos',
        route: '/modules',
        icon: 'Package',
        order: 4,
        permissions: ['modules.read', 'modules.create', 'modules.update', 'modules.delete'],
      },
      {
        name: 'imports',
        description: 'Importación Masiva',
        route: '/imports',
        icon: 'Upload',
        order: 5,
        permissions: ['imports.read', 'imports.create', 'imports.update', 'imports.delete'],
      },
      {
        name: 'audit',
        description: 'Auditoría y Logs',
        route: '/audit',
        icon: 'History',
        order: 6,
        permissions: ['audit.read'],
      },
      {
        name: 'configuration',
        description: 'Configuración del Sistema',
        route: '/configuration',
        icon: 'Settings',
        order: 7,
        permissions: ['configuration.read', 'configuration.update'],
      },
      {
        name: 'sessions',
        description: 'Gestión de Sesiones',
        route: '/sessions',
        icon: 'Monitor',
        order: 8,
        permissions: ['sessions.read', 'sessions.delete'],
      },
    ];

    for (const defaultModule of defaultModules) {
      const existing = await this.findByName(defaultModule.name);
      
      if (!existing) {
        await this.create(defaultModule);
        this.logger.log(`⚙️ Módulo por defecto creado: ${defaultModule.name}`);
      }
    }
  }

  async getModulesForUser(userId: number, userRoles: string[]): Promise<ModuleEntity[]> {
    // Obtener todos los módulos activos
    const allModules = await this.findActive();
    
    // Filtrar módulos según permisos del usuario
    // Esta es una implementación básica, se puede expandir según necesidades
    return allModules.filter(module => {
      // Si el usuario tiene rol superadmin, acceso a todo
      if (userRoles.includes('superadmin')) {
        return true;
      }
      
      // Verificar permisos específicos del módulo
      if (module.permissions && module.permissions.length > 0) {
        return module.permissions.some(permission => userRoles.includes(permission));
      }
      
      return true;
    });
  }

  async registerModule(moduleConfig: CreateModuleDto): Promise<ModuleEntity> {
    // Registrar un nuevo módulo dinámicamente
    return this.create(moduleConfig);
  }

  async unregisterModule(moduleName: string): Promise<void> {
    const module = await this.findByName(moduleName);
    if (module) {
      await this.remove(module.id);
    }
  }
} 