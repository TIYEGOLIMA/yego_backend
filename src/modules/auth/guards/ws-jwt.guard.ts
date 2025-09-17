import { CanActivate, Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { WsException } from '@nestjs/websockets';
import { Socket } from 'socket.io';

@Injectable()
export class WsJwtGuard implements CanActivate {
  constructor(private jwtService: JwtService) {}

  async canActivate(context: any): Promise<boolean> {
    try {
      const client: Socket = context.switchToWs().getClient();
      const token = this.extractTokenFromHeader(client);
      
      if (!token) {
        throw new WsException('Token no proporcionado');
      }

      const payload = await this.jwtService.verifyAsync(token);
      
      // Asignar usuario al socket para uso posterior
      (client as any).user = payload;
      
      return true;
    } catch (error) {
      throw new WsException('Token inválido');
    }
  }

  private extractTokenFromHeader(client: Socket): string | undefined {
    const auth = client.handshake.auth?.token || 
                 client.handshake.headers?.authorization;
    if (!auth) {
      return undefined;
    }
    if (auth.startsWith('Bearer ')) {
      return auth.substring(7);
    }
    return auth;
  }
} 