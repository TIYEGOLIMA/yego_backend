import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Role } from './entities/role.entity';
import { User } from '../users/entities/user.entity';
import { Logger } from '@nestjs/common';
import { WebsocketGateway } from '../websocket/websocket.gateway';

@Injectable()
export class RolesService {
  private readonly logger = new Logger(RolesService.name);

  constructor(
    @InjectRepository(Role)
    private roleRepository: Repository<Role>,
    @InjectRepository(User)
    private userRepository: Repository<User>,
    private websocketGateway: WebsocketGateway,
  ) {}

  async create(createRoleDto: { name: string; description?: string; permissions?: Record<string, any> }): Promise<Role> {
    // Verificar si el rol ya existe
    const existingRole = await this.roleRepository.findOne({
      where: { name: createRoleDto.name },
    });

    if (existingRole) {
      throw new ConflictException(`El rol '${createRoleDto.name}' ya existe`);
    }

    const role = this.roleRepository.create(createRoleDto);
    const savedRole = await this.roleRepository.save(role);
    
    this.logger.log(`✅ Rol creado: ${savedRole.name}`);
    
    return savedRole;
  }

  async findAll(): Promise<Role[]> {
    return this.roleRepository.find({
      order: { name: 'ASC' },
    });
  }

  async findOne(id: number): Promise<Role> {
    const role = await this.roleRepository.findOne({
      where: { id },
      relations: ['users'],
    });

    if (!role) {
      throw new NotFoundException(`Rol con ID ${id} no encontrado`);
    }

    return role;
  }

  async findByName(name: string): Promise<Role | null> {
    return this.roleRepository.findOne({
      where: { name },
    });
  }

  async update(id: number, updateRoleDto: { name?: string; description?: string; permissions?: Record<string, any> }): Promise<Role> {
    // Buscar el rol y cargar la relación de usuarios
    const role = await this.roleRepository.findOne({
      where: { id },
      relations: ['users'],
    });
    if (!role) {
      throw new NotFoundException(`Rol con ID ${id} no encontrado`);
    }

    // Verificar si el nuevo nombre ya existe (si se está cambiando)
    if (updateRoleDto.name && updateRoleDto.name !== role.name) {
      const existingRole = await this.roleRepository.findOne({
        where: { name: updateRoleDto.name },
      });
      if (existingRole) {
        throw new ConflictException(`El rol '${updateRoleDto.name}' ya existe`);
      }
    }

    Object.assign(role, updateRoleDto);
    const savedRole = await this.roleRepository.save(role);
    
    this.logger.log(`✅ Rol actualizado: ${savedRole.name}`);
    this.logger.log(`📝 Permisos guardados: ${JSON.stringify(savedRole.permissions, null, 2)}`);
    
    // Emitir evento a los usuarios afectados por el cambio de rol
    const usersWithRole = await this.userRepository.find({ where: { role: role.name } });
    if (usersWithRole.length > 0) {
      for (const user of usersWithRole) {
        this.logger.log(`Enviando permissions-updated a userId: ${user.id}`);
        this.websocketGateway.emitToUser(user.id, 'permissions-updated', { roleId: role.id });
      }
    }
    
    return savedRole;
  }

  async remove(id: number): Promise<void> {
    const role = await this.findOne(id);
    
    // Verificar si el rol tiene usuarios asignados
    const userCount = await this.getUserCountByRole(role.name);
    if (userCount > 0) {
      throw new ConflictException(`No se puede eliminar el rol '${role.name}' porque tiene ${userCount} usuarios asignados`);
    }

    await this.roleRepository.remove(role);
    
    this.logger.log(`🗑️ Rol eliminado: ${role.name}`);
  }

  async getDefaultRoles(): Promise<Role[]> {
    return this.roleRepository.find({
      where: [
        { name: 'superadmin' },
        { name: 'admin' },
        { name: 'supervisor' },
        { name: 'operador' },
        { name: 'conductor' },
        { name: 'agent' },
      ],
    });
  }

  async initializeDefaultRoles(): Promise<void> {
    const defaultRoles = [
      {
        name: 'superadmin',
        description: 'Super Administrador del Sistema',
        permissions: { all: true },
      },
      {
        name: 'admin',
        description: 'Administrador',
        permissions: {
          users: ['read', 'write', 'delete'],
          roles: ['read', 'write'],
          modules: ['read', 'write'],
          imports: ['read', 'write'],
          audit: ['read'],
          configuration: ['read', 'write'],
        },
      },
      {
        name: 'supervisor',
        description: 'Supervisor',
        permissions: {
          users: ['read'],
          imports: ['read', 'write'],
          audit: ['read'],
        },
      },
      {
        name: 'operador',
        description: 'Operador',
        permissions: {
          imports: ['read', 'write'],
          tickets: ['read', 'write'],
        },
      },
      {
        name: 'conductor',
        description: 'Conductor',
        permissions: {
          profile: ['read', 'write'],
        },
      },
      {
        name: 'agent',
        description: 'Agente de Soporte',
        permissions: {
          tickets: ['read', 'write'],
        },
      },
    ];

    for (const defaultRole of defaultRoles) {
      const existing = await this.findByName(defaultRole.name);
      
      if (!existing) {
        await this.create(defaultRole);
        this.logger.log(`⚙️ Rol por defecto creado: ${defaultRole.name}`);
      }
    }
  }

  // async activate(id: number): Promise<Role> {
  //   const role = await this.findOne(id);
  //   (role as any).activo = true;
  //   const savedRole = await this.roleRepository.save(role);
  //   
  //   this.logger.log(`✅ Rol activado: ${savedRole.name}`);
  //   
  //   return savedRole;
  // }

  // async deactivate(id: number): Promise<Role> {
  //   const role = await this.findOne(id);
  //   (role as any).activo = false;
  //   const savedRole = await this.roleRepository.save(role);
  //   
  //   this.logger.log(`❌ Rol desactivado: ${savedRole.name}`);
  //   
  //   return savedRole;
  // }

  async getUserCountByRole(roleName: string): Promise<number> {
    return this.userRepository.count({ where: { role: roleName } });
  }
} 