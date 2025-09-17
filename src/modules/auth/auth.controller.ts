import { Controller, Post, Body, UseGuards, Get, Request, UnauthorizedException, BadRequestException } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { ChangePasswordDto } from './dto/change-password.dto';
import { LocalAuthGuard } from './guards/local-auth.guard';
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
} 