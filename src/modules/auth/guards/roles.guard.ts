import { Injectable, CanActivate, ExecutionContext } from '@nestjs/common';
import { Reflector } from '@nestjs/core';

@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const requiredRoles = this.reflector.getAllAndOverride<string[]>('roles', [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!requiredRoles) {
      return true;
    }

    const { user } = context.switchToHttp().getRequest();
    
    if (!user) {
      return false;
    }

    // Extraer nombres de roles del usuario
    const userRoleNames = user.roles?.map(role => role.name) || [];

    // Verificar si el usuario tiene el rol superadmin (acceso total)
    if (userRoleNames.includes('superadmin')) {
      return true;
    }

    // Verificar si el usuario tiene alguno de los roles requeridos
    return requiredRoles.some(role => userRoleNames.includes(role));
  }
} 