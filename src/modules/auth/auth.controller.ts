import { Controller, Post, Body, UseGuards, Get, Request, UnauthorizedException, BadRequestException } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { ChangePasswordDto } from './dto/change-password.dto';
import { JwtAuthGuard } from './guards/jwt-auth.guard';

@ApiTags('Autenticación')
@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('login')
  @ApiOperation({ summary: 'Iniciar sesión' })
  @ApiResponse({ status: 200, description: 'Login exitoso' })
  @ApiResponse({ status: 401, description: 'Credenciales inválidas' })
  async login(@Body() loginDto: LoginDto, @Request() req) {
    const user = await this.authService.validateUser(
      loginDto.username,
      loginDto.password,
      req,
    );
    
    if (!user) {
      throw new UnauthorizedException('Credenciales inválidas');
    }
    
    return this.authService.login(user, req);
  }

  @Post('register')
  @ApiOperation({ summary: 'Registrar nuevo usuario' })
  @ApiResponse({ status: 201, description: 'Usuario registrado exitosamente' })
  @ApiResponse({ status: 409, description: 'Usuario o email ya existe' })
  async register(@Body() registerDto: RegisterDto) {
    return this.authService.register(registerDto);
  }


  @Get('profile')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Obtener perfil del usuario autenticado' })
  @ApiResponse({ status: 200, description: 'Perfil obtenido exitosamente' })
  @ApiResponse({ status: 401, description: 'No autorizado' })
  async getProfile(@Request() req) {
    // Obtener el usuario actualizado de la base de datos con roles y permisos
    const user = await this.authService.getUserProfile(req.user.id);
    console.log('📤 Perfil devuelto por API:', JSON.stringify(user, null, 2));
    return user;
  }

  @Post('change-password')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Cambiar contraseña del usuario autenticado' })
  @ApiResponse({ status: 200, description: 'Contraseña cambiada exitosamente' })
  @ApiResponse({ status: 400, description: 'Datos inválidos' })
  @ApiResponse({ status: 401, description: 'No autorizado' })
  async changePassword(@Body() changePasswordDto: ChangePasswordDto, @Request() req) {
    // Validar que las contraseñas coincidan
    if (changePasswordDto.newPassword !== changePasswordDto.confirmPassword) {
      throw new BadRequestException('Las contraseñas no coinciden');
    }

    return this.authService.changePassword(
      req.user.id,
      changePasswordDto.currentPassword,
      changePasswordDto.newPassword
    );
  }

  @Post('reset-password')
  @ApiOperation({ summary: 'Cambiar contraseña inicial' })
  @ApiResponse({ status: 200, description: 'Contraseña cambiada exitosamente' })
  @ApiResponse({ status: 400, description: 'Datos inválidos' })
  @ApiResponse({ status: 401, description: 'Credenciales inválidas' })
  async resetPassword(@Body() changePasswordDto: ChangePasswordDto) {
    // Validar que las contraseñas coincidan
    if (changePasswordDto.newPassword !== changePasswordDto.confirmPassword) {
      throw new BadRequestException('Las contraseñas no coinciden');
    }

    // Buscar usuario por username, email o DNI
    const user = await this.authService.validateUser(
      changePasswordDto.username,
      changePasswordDto.currentPassword,
    );

    if (!user) {
      throw new UnauthorizedException('Credenciales inválidas');
    }

    // Verificar que el usuario requiera cambio de contraseña
    if (!user.requiereCambioPassword) {
      throw new BadRequestException('Este usuario no requiere cambio de contraseña');
    }

    return this.authService.changePassword(
      user.id,
      changePasswordDto.currentPassword,
      changePasswordDto.newPassword
    );
  }

  @Post('logout')
  @ApiOperation({ summary: 'Cerrar sesión completa y liberar módulo' })
  @ApiResponse({ status: 200, description: 'Logout exitoso, módulo liberado' })
  @ApiResponse({ status: 400, description: 'Error al cerrar sesión' })
  async cerrarSesion(@Request() req) {
    console.log(`🔓 Iniciando logout`);
    
    try {
      // Extraer token del header Authorization
      const authHeader = req.headers.authorization;
      const token = authHeader?.replace('Bearer ', '') || '';
      
      if (!token) {
        return { 
          message: 'Logout exitoso - no hay token activo',
          success: true,
          timestamp: new Date().toISOString()
        };
      }

      // Intentar obtener información del usuario del token (incluso si está expirado)
      let userId = null;
      let username = 'usuario';
      
      try {
        // Decodificar token sin verificar (para obtener info incluso si expiró)
        const decoded = this.authService.decodeToken(token);
        userId = decoded?.sub || decoded?.userId;
        username = decoded?.username || 'usuario';
        console.log(`🔓 Usuario ${username} (ID: ${userId}) iniciando logout completo`);
      } catch (decodeError) {
        console.log('⚠️ No se pudo decodificar el token, continuando con logout genérico');
      }

      await this.authService.cerrarSesion(userId, token);
      
      return { 
        message: 'Logout exitoso, módulo liberado',
        success: true,
        timestamp: new Date().toISOString(),
        user: username
      };
    } catch (error) {
      console.error(`❌ Error en logout:`, error);
      // Siempre devolver éxito en logout para evitar loops infinitos
      return { 
        message: 'Logout completado con advertencias',
        success: true,
        timestamp: new Date().toISOString(),
        warning: 'Algunos recursos no pudieron ser liberados'
      };
    }
  }

  @Post('force-logout')
  @ApiOperation({ summary: 'Forzar logout sin autenticación (para casos de emergencia)' })
  @ApiResponse({ status: 200, description: 'Logout forzado exitoso' })
  async forceLogout(@Request() req) {
    console.log(`🚨 Logout forzado solicitado`);
    
    try {
      // Extraer token del header Authorization si existe
      const authHeader = req.headers.authorization;
      const token = authHeader?.replace('Bearer ', '') || '';
      
      if (token) {
        // Intentar liberar recursos con el token disponible
        await this.authService.cerrarSesion(null, token);
      }
      
      return { 
        message: 'Logout forzado exitoso',
        success: true,
        timestamp: new Date().toISOString()
      };
    } catch (error) {
      console.error(`❌ Error en logout forzado:`, error);
      // Siempre devolver éxito para evitar loops
      return { 
        message: 'Logout forzado completado',
        success: true,
        timestamp: new Date().toISOString()
      };
    }
  }
} 