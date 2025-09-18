import { Injectable, UnauthorizedException, ConflictException, BadRequestException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { User } from '../users/entities/user.entity';
import { RegisterDto } from './dto/register.dto';
import { SessionsService } from '../sessions/sessions.service';
import { CreateSessionDto } from '../sessions/dto/create-session.dto';
import { AuditService } from '../audit/audit.service';

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(User)
    private userRepository: Repository<User>,
    private jwtService: JwtService,
    private sessionsService: SessionsService,
    private auditService: AuditService,
  ) {}

  async validateUser(username: string, password: string, request?: any): Promise<any> {
    // Buscar usuario por username o email
    const user = await this.userRepository.findOne({
      where: [
        { username },
        { email: username }
      ],
    });

    if (user && await bcrypt.compare(password, user.password)) {
      const { password: _, ...result } = user;
      return result;
    } else if (user) {
      // Registrar intento fallido de login
      await this.auditService.logFailedLogin(username, request?.ip, request?.headers?.['user-agent']);
    }
    return null;
  }

  async login(user: any, request?: any) {
    // Actualizar last_login
    await this.userRepository.update(user.id, {
      lastLogin: new Date()
    });

    const payload = { username: user.username, sub: user.id, userId: user.id, role: user.role };
    const accessToken = this.jwtService.sign(payload);
    
    // Create session with geolocation
    const sessionDto: CreateSessionDto = {
      tokenHash: await bcrypt.hash(accessToken, 12),
      userAgent: request?.headers?.['user-agent'],
      expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(), // 24 hours
    };

    await this.sessionsService.create(sessionDto, user.id, request);

    // Registrar login exitoso en auditoría
    await this.auditService.logLogin(user.id, request?.ip, request?.headers?.['user-agent']);

    return {
      access_token: accessToken,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        name: user.name,
        role: user.role,
        moduleId: user.moduleId,
        active: user.active,
      },
    };
  }

  async register(registerDto: RegisterDto) {
    // Check if user already exists
    const existingUser = await this.userRepository.findOne({
      where: [
        { username: registerDto.username },
        { email: registerDto.email },
      ],
    });

    if (existingUser) {
      throw new ConflictException('El usuario o email ya existe');
    }

    // Hash password
    const saltRounds = 12;
    const passwordHash = await bcrypt.hash(registerDto.password, saltRounds);

    // Create user
    const user = this.userRepository.create({
      username: registerDto.username,
      email: registerDto.email,
      name: registerDto.nombre || registerDto.username,
      password: passwordHash,
      role: 'usuario',
      active: true,
    });

    const savedUser = await this.userRepository.save(user);
    const { password: _, ...result } = savedUser;

    return result;
  }

  async changePassword(userId: number, currentPassword: string, newPassword: string): Promise<any> {
    // Buscar el usuario
    const user = await this.userRepository.findOne({
      where: { id: userId },
    });

    if (!user) {
      throw new UnauthorizedException('Usuario no encontrado');
    }

    // Verificar contraseña actual
    const isCurrentPasswordValid = await bcrypt.compare(currentPassword, user.password);
    if (!isCurrentPasswordValid) {
      throw new UnauthorizedException('Contraseña actual incorrecta');
    }

    // Validar nueva contraseña
    if (!this.validatePassword(newPassword)) {
      throw new BadRequestException('La nueva contraseña no cumple con los requisitos de seguridad');
    }

    // Verificar que la nueva contraseña no sea igual a la actual
    const isSamePassword = await bcrypt.compare(newPassword, user.password);
    if (isSamePassword) {
      throw new BadRequestException('La nueva contraseña no puede ser igual a la actual');
    }

    // Hash nueva contraseña
    const saltRounds = 12;
    const newPasswordHash = await bcrypt.hash(newPassword, saltRounds);

    // Actualizar contraseña
    await this.userRepository.update(userId, {
      password: newPasswordHash
    });

    return {
      message: 'Contraseña cambiada exitosamente',
    };
  }

  async validatePassword(password: string): Promise<boolean> {
    // Check for weak passwords
    const weakPasswords = ['123456', 'admin', 'password', '123456789', 'qwerty'];
    if (weakPasswords.includes(password.toLowerCase())) {
      return false;
    }

    // Check for minimum requirements
    const hasUpperCase = /[A-Z]/.test(password);
    const hasLowerCase = /[a-z]/.test(password);
    const hasNumbers = /\d/.test(password);
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(password);
    const isLongEnough = password.length >= 8;

    return hasUpperCase && hasLowerCase && hasNumbers && hasSpecialChar && isLongEnough;
  }

  async getUserProfile(userId: number) {
    const user = await this.userRepository.findOne({
      where: { id: userId },
    });

    if (!user) {
      throw new UnauthorizedException('Usuario no encontrado');
    }

    return {
      id: user.id,
      username: user.username,
      email: user.email,
      name: user.name,
      role: user.role,
      moduleId: user.moduleId,
      active: user.active,
      lastLogin: user.lastLogin,
    };
  }

  async cerrarSesion(userId: number, token: string): Promise<void> {
    console.log(`🔄 Usuario ${userId} cerrando sesión`);
    
    try {
      const user = await this.userRepository.findOne({ 
        where: { id: userId } 
      });

      if (!user) {
        console.warn(`⚠️ Usuario ${userId} no encontrado para logout`);
        return;
      }

      // 🆕 LIBERAR MÓDULO EN BACKEND EXTERNO (igual que en frontend)
      try {
        console.log('🔄 [AuthService] Liberando módulo asignado en backend...');
        
        const backendUrl = 'http://10.10.12.117:3030/api';
        const response = await fetch(`${backendUrl}/auth/logout`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          signal: AbortSignal.timeout(5000) // 5 segundos de timeout
        });
        
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        console.log('✅ [AuthService] Módulo liberado exitosamente en backend');
      } catch (moduleError) {
        console.warn('⚠️ [AuthService] No se pudo liberar módulo en backend:', moduleError.message);
        // Continuar con el logout aunque falle la liberación del módulo
      }

      console.log(`✅ Logout completado para usuario ${user.username} (ID: ${userId})`);
      
    } catch (error) {
      console.error(`❌ Error en logout para usuario ${userId}:`, error);
      throw new BadRequestException('Error al cerrar sesión');
    }
  }
} 