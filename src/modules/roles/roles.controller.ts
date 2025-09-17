import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Delete,
  Put,
  Patch,
  UseGuards,
  Query,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { RolesService } from './roles.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { Logger } from '@nestjs/common';

@ApiTags('Roles')
@Controller('roles')
@UseGuards(JwtAuthGuard, RolesGuard)
@ApiBearerAuth()
export class RolesController {
  private readonly logger = new Logger(RolesController.name);

  constructor(private readonly rolesService: RolesService) {}

  @Post()
  @ApiOperation({ summary: 'Crear nuevo rol' })
  @ApiResponse({ status: 201, description: 'Rol creado exitosamente' })
  @ApiResponse({ status: 400, description: 'Datos inválidos' })
  @ApiResponse({ status: 409, description: 'El rol ya existe' })
  @Roles('admin', 'superadmin')
  async create(@Body() createRoleDto: { name: string; description?: string; permissions?: Record<string, any> }) {
    const role = await this.rolesService.create(createRoleDto);
    this.logger.log(`✅ Rol creado via API: ${role.name}`);
    return {
      message: 'Rol creado exitosamente',
      role,
    };
  }

  @Get()
  @ApiOperation({ summary: 'Obtener todos los roles' })
  @ApiResponse({ status: 200, description: 'Lista de roles' })
  @Roles('admin', 'superadmin')
  async findAll(@Query('includeUsers') includeUsers?: string) {
    const roles = await this.rolesService.findAll();
    
    if (includeUsers === 'true') {
      // Contar usuarios por rol usando consulta directa
      const rolesWithUsers = await Promise.all(
        roles.map(async (role) => {
          const userCount = await this.rolesService.getUserCountByRole(role.name);
          return {
            ...role,
            user_count: userCount,
          };
        })
      );
      return rolesWithUsers;
    }
    
    return roles;
  }

  @Get('default')
  @ApiOperation({ summary: 'Obtener roles por defecto' })
  @ApiResponse({ status: 200, description: 'Roles por defecto' })
  @Roles('admin')
  async getDefaultRoles() {
    return this.rolesService.getDefaultRoles();
  }

  @Get(':id')
  @ApiOperation({ summary: 'Obtener rol por ID' })
  @ApiResponse({ status: 200, description: 'Rol encontrado' })
  @ApiResponse({ status: 404, description: 'Rol no encontrado' })
  @Roles('admin', 'superadmin')
  async findOne(@Param('id') id: string) {
    const role = await this.rolesService.findOne(+id);
    const userCount = await this.rolesService.getUserCountByRole(role.name);
    return {
      role,
      user_count: userCount,
    };
  }

  @Put(':id')
  @ApiOperation({ summary: 'Actualizar rol' })
  @ApiResponse({ status: 200, description: 'Rol actualizado exitosamente' })
  @ApiResponse({ status: 404, description: 'Rol no encontrado' })
  @ApiResponse({ status: 409, description: 'El nombre del rol ya existe' })
  @Roles('admin', 'superadmin')
  async update(
    @Param('id') id: string,
    @Body() updateRoleDto: { name?: string; description?: string; permissions?: Record<string, any> },
  ) {
    const role = await this.rolesService.update(+id, updateRoleDto);
    this.logger.log(`✅ Rol actualizado via API: ${role.name}`);
    return {
      message: 'Rol actualizado exitosamente',
      role,
    };
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Eliminar rol' })
  @ApiResponse({ status: 200, description: 'Rol eliminado exitosamente' })
  @ApiResponse({ status: 404, description: 'Rol no encontrado' })
  @ApiResponse({ status: 409, description: 'No se puede eliminar el rol porque tiene usuarios asignados' })
  @Roles('admin', 'superadmin')
  async remove(@Param('id') id: string) {
    await this.rolesService.remove(+id);
    this.logger.log(`🗑️ Rol eliminado via API: ID ${id}`);
    return {
      message: 'Rol eliminado exitosamente',
    };
  }

  @Post('initialize')
  @ApiOperation({ summary: 'Inicializar roles por defecto' })
  @ApiResponse({ status: 201, description: 'Roles inicializados exitosamente' })
  @Roles('superadmin')
  async initializeDefaultRoles() {
    await this.rolesService.initializeDefaultRoles();
    this.logger.log('⚙️ Roles por defecto inicializados via API');
    return {
      message: 'Roles por defecto inicializados exitosamente',
    };
  }

  // @Patch(':id/activate')
  // @ApiOperation({ summary: 'Activar rol' })
  // @ApiResponse({ status: 200, description: 'Rol activado exitosamente' })
  // @ApiResponse({ status: 404, description: 'Rol no encontrado' })
  // @Roles('admin', 'superadmin')
  // async activate(@Param('id') id: string) {
  //   const role = await this.rolesService.activate(+id);
  //   this.logger.log(`✅ Rol activado via API: ${role.name}`);
  //   return {
  //     message: 'Rol activado exitosamente',
  //     role,
  //   };
  // }

  // @Patch(':id/deactivate')
  // @ApiOperation({ summary: 'Desactivar rol' })
  // @ApiResponse({ status: 200, description: 'Rol desactivado exitosamente' })
  // @ApiResponse({ status: 404, description: 'Rol no encontrado' })
  // @Roles('admin', 'superadmin')
  // async deactivate(@Param('id') id: string) {
  //   const role = await this.rolesService.deactivate(+id);
  //   this.logger.log(`❌ Rol desactivado via API: ${role.name}`);
  //   return {
  //     message: 'Rol desactivado exitosamente',
  //     role,
  //   };
  // }
} 