import { ConfigService } from '@nestjs/config';
import { JwtModuleOptions } from '@nestjs/jwt';

export const getJwtConfig = (configService: ConfigService): JwtModuleOptions => {
  const jwtSecret = configService.get('JWT_SECRET');
  
  if (!jwtSecret) {
    throw new Error(
      'JWT_SECRET no está configurado. Variables de entorno requeridas: JWT_SECRET, JWT_EXPIRES_IN (opcional)'
    );
  }

  return {
    secret: jwtSecret,
    signOptions: {
      expiresIn: configService.get('JWT_EXPIRES_IN', '3600s'),
    },
  };
}; 