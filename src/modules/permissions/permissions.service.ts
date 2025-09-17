import { Injectable, Logger, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Permission } from './entities/permission.entity';
import { CreatePermissionDto } from './dto/create-permission.dto';
import { UpdatePermissionDto } from './dto/update-permission.dto';

@Injectable()
export class PermissionsService {
  private readonly logger = new Logger(PermissionsService.name);

  constructor(
    @InjectRepository(Permission)
    private permissionRepository: Repository<Permission>,
  ) {}

  async create(createPermissionDto: CreatePermissionDto): Promise<Permission> {
    // Verificar si el permiso ya existe
    const existingPermission = await this.permissionRepository.findOne({
      where: { name: createPermissionDto.name },
    });

    if (existingPermission) {
      throw new ConflictException(`El permiso '${createPermissionDto.name}' ya existe`);
    }

    const permission = this.permissionRepository.create(createPermissionDto);
    const savedPermission = await this.permissionRepository.save(permission);
    
    this.logger.log(`✅ Permiso creado: ${savedPermission.name}`);
    
    return savedPermission;
  }

  async findAll(): Promise<Permission[]> {
    return this.permissionRepository.find({
      where: { active: true },
      order: { module: 'ASC', action: 'ASC' },
    });
  }

  async findByModule(module: string): Promise<Permission[]> {
    return this.permissionRepository.find({
      where: { module, active: true },
      order: { action: 'ASC' },
    });
  }

  async findOne(id: number): Promise<Permission> {
    const permission = await this.permissionRepository.findOne({
      where: { id },
    });

    if (!permission) {
      throw new NotFoundException(`Permiso con ID ${id} no encontrado`);
    }

    return permission;
  }

  async findByName(name: string): Promise<Permission | null> {
    return this.permissionRepository.findOne({
      where: { name },
    });
  }

  async update(id: number, updatePermissionDto: UpdatePermissionDto): Promise<Permission> {
    const permission = await this.findOne(id);

    // Verificar si el nuevo nombre ya existe (si se está cambiando)
    if (updatePermissionDto.name && updatePermissionDto.name !== permission.name) {
      const existingPermission = await this.findByName(updatePermissionDto.name);
      if (existingPermission) {
        throw new ConflictException(`El permiso '${updatePermissionDto.name}' ya existe`);
      }
    }

    Object.assign(permission, updatePermissionDto);
    const savedPermission = await this.permissionRepository.save(permission);
    
    this.logger.log(`✅ Permiso actualizado: ${savedPermission.name}`);
    
    return savedPermission;
  }

  async remove(id: number): Promise<void> {
    const permission = await this.findOne(id);
    
    // Soft delete
    permission.active = false;
    await this.permissionRepository.save(permission);
    
    this.logger.log(`✅ Permiso desactivado: ${permission.name}`);
  }

  async initializeDefaultPermissions(): Promise<void> {
    const defaultPermissions = [
      // Users module
      { name: 'users.create', description: 'Crear usuarios', module: 'users', action: 'create' },
      { name: 'users.read', description: 'Ver usuarios', module: 'users', action: 'read' },
      { name: 'users.update', description: 'Actualizar usuarios', module: 'users', action: 'update' },
      { name: 'users.delete', description: 'Eliminar usuarios', module: 'users', action: 'delete' },
      
      // Roles module
      { name: 'roles.create', description: 'Crear roles', module: 'roles', action: 'create' },
      { name: 'roles.read', description: 'Ver roles', module: 'roles', action: 'read' },
      { name: 'roles.update', description: 'Actualizar roles', module: 'roles', action: 'update' },
      { name: 'roles.delete', description: 'Eliminar roles', module: 'roles', action: 'delete' },
      
      // Permissions module
      { name: 'permissions.create', description: 'Crear permisos', module: 'permissions', action: 'create' },
      { name: 'permissions.read', description: 'Ver permisos', module: 'permissions', action: 'read' },
      { name: 'permissions.update', description: 'Actualizar permisos', module: 'permissions', action: 'update' },
      { name: 'permissions.delete', description: 'Eliminar permisos', module: 'permissions', action: 'delete' },
      
      // Modules module
      { name: 'modules.create', description: 'Crear módulos', module: 'modules', action: 'create' },
      { name: 'modules.read', description: 'Ver módulos', module: 'modules', action: 'read' },
      { name: 'modules.update', description: 'Actualizar módulos', module: 'modules', action: 'update' },
      { name: 'modules.delete', description: 'Eliminar módulos', module: 'modules', action: 'delete' },
      
      // Imports module
      { name: 'imports.create', description: 'Crear importaciones', module: 'imports', action: 'create' },
      { name: 'imports.read', description: 'Ver importaciones', module: 'imports', action: 'read' },
      { name: 'imports.update', description: 'Actualizar importaciones', module: 'imports', action: 'update' },
      { name: 'imports.delete', description: 'Eliminar importaciones', module: 'imports', action: 'delete' },
      
      // Audit module
      { name: 'audit.read', description: 'Ver logs de auditoría', module: 'audit', action: 'read' },
      
      // Configuration module
      { name: 'configuration.read', description: 'Ver configuración', module: 'configuration', action: 'read' },
      { name: 'configuration.update', description: 'Actualizar configuración', module: 'configuration', action: 'update' },
      
      // Sessions module
      { name: 'sessions.read', description: 'Ver sesiones', module: 'sessions', action: 'read' },
      { name: 'sessions.delete', description: 'Cerrar sesiones', module: 'sessions', action: 'delete' },
    ];

    for (const defaultPermission of defaultPermissions) {
      const existing = await this.findByName(defaultPermission.name);
      
      if (!existing) {
        await this.create(defaultPermission);
        this.logger.log(`⚙️ Permiso por defecto creado: ${defaultPermission.name}`);
      }
    }
  }

  async checkPermission(userId: number, permissionName: string): Promise<boolean> {
    // Implementar lógica de verificación de permisos
    // Esta es una implementación básica, se puede expandir según necesidades
    return true;
  }
} 