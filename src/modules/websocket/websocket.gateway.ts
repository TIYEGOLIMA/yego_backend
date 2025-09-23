import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  OnGatewayConnection,
  OnGatewayDisconnect,
  ConnectedSocket,
  MessageBody,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { UseGuards } from '@nestjs/common';
import { WsJwtGuard } from '../auth/guards/ws-jwt.guard';
import { JwtService } from '@nestjs/jwt';

@WebSocketGateway({
  namespace: 'ws',
  cors: {
    origin: [
      // URLs de desarrollo
      process.env.FRONTEND_URL || 'http://localhost:3000',
      'http://localhost:5173',
      'http://127.0.0.1:5173',
      
      // URLs de producción HTTP (legacy)
      'http://5.161.86.63:4000',
      'http://5.161.86.63:5173',
      'http://5.161.86.63:80',
      'http://5.161.86.63',
      
      // URLs de producción HTTPS (principales)
      'https://integral.yego.pro',
      'https://api-int.yego.pro',
      'https://yego.pro',
      'https://www.yego.pro',
      
      // Variable de entorno personalizada
      process.env.FRONTEND_PROD_URL,
    ].filter(Boolean), // Filtra valores undefined
    credentials: true,
    methods: ['GET', 'POST'],
  },
})
@UseGuards(WsJwtGuard)
export class WebsocketGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private connectedClients = new Map<string, { socket: Socket; userId: number; sessionId: string }>();

  constructor(private readonly jwtService: JwtService) {}

  async handleConnection(client: Socket) {
    try {
      console.log(`🔌 Cliente intentando conectar: ${client.id}`);
      
      // Ejecutar el guard manualmente
      const guard = new WsJwtGuard(this.jwtService);
      const context = { switchToWs: () => ({ getClient: () => client }) };
      const isAllowed = await guard.canActivate(context);
      if (!isAllowed) {
        console.log(`❌ Cliente ${client.id} rechazado por autenticación`);
        client.disconnect();
        return;
      }
      const user = (client as any).user;
      const userId = user.id || user.sub;
      this.connectedClients.set(client.id, {
        socket: client,
        userId: userId,
        sessionId: user.sessionId || client.id,
      });
      client.join(`user:${userId}`);
      console.log(`✅ Cliente ${client.id} conectado para usuario ${userId}`);
      client.emit('connection-established', {
        userId: userId,
        sessionId: user.sessionId || client.id,
        timestamp: new Date().toISOString(),
      });
    } catch (error) {
      console.log(`❌ Error en conexión del cliente ${client.id}:`, error);
      client.disconnect();
    }
  }

  handleDisconnect(client: Socket) {
    try {
      console.log(`🔌 Cliente desconectado: ${client.id}`);
      // Remover cliente de la lista
      this.connectedClients.delete(client.id);
      
    } catch (error) {
      console.log(`❌ Error en desconexión del cliente ${client.id}:`, error);
    }
  }

  @SubscribeMessage('register-session')
  async handleRegisterSession(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { sessionId: string },
  ) {
    try {
      const user = (client as any).user;
      if (!user) {
        console.log(`❌ Intento de registro de sesión sin usuario: ${client.id}`);
        client.emit('error', { message: 'Usuario no autenticado' });
        return;
      }

      const sessionId = data.sessionId;
      console.log(`📝 Registrando sesión ${sessionId} para cliente ${client.id}`);

      // Actualizar sessionId en el cliente
      const clientData = this.connectedClients.get(client.id);
      if (clientData) {
        clientData.sessionId = sessionId;
        this.connectedClients.set(client.id, clientData);
      }

      // Unirse a sala de sesión
      client.join(`session:${sessionId}`);

      client.emit('session-registered', {
        sessionId,
        userId: user.id,
        timestamp: new Date().toISOString(),
      });

      console.log(`✅ Sesión ${sessionId} registrada exitosamente para cliente ${client.id}`);

    } catch (error) {
      console.log(`❌ Error registrando sesión para cliente ${client.id}:`, error);
      client.emit('error', { message: 'Error registrando sesión' });
    }
  }

  @SubscribeMessage('ping')
  handlePing(@ConnectedSocket() client: Socket) {
    client.emit('pong', { timestamp: new Date().toISOString() });
  }

  // Métodos públicos para emitir eventos desde otros servicios
  emitToUser(userId: number, event: string, data: any) {
    console.log(`📡 Emitiendo evento ${event} a usuario ${userId} con datos:`, data);
    this.server.to(`user:${userId}`).emit(event, data);
  }

  emitToSession(sessionId: string, event: string, data: any) {
    this.server.to(`session:${sessionId}`).emit(event, data);
  }

  emitToAll(event: string, data: any) {
    this.server.emit(event, data);
  }

  // Método para cerrar sesión específica
  closeSession(sessionId: string, reason: string = 'Sesión cerrada por administrador') {
    this.server.to(`session:${sessionId}`).emit('session-closed', {
      sessionId,
      reason,
      timestamp: new Date().toISOString(),
    });

    // Desconectar clientes de esta sesión
    this.server.in(`session:${sessionId}`).disconnectSockets();
  }

  // Método para forzar logout de usuario
  forceLogout(userId: number, reason: string = 'Sesión cerrada por seguridad') {
    this.server.to(`user:${userId}`).emit('force-logout', {
      userId,
      reason,
      timestamp: new Date().toISOString(),
    });

    // Desconectar clientes del usuario
    this.server.in(`user:${userId}`).disconnectSockets();
  }

  // Obtener estadísticas de conexiones
  getConnectionStats() {
    const stats = {
      totalConnections: this.connectedClients.size,
      users: new Set(),
      sessions: new Set(),
    };

    this.connectedClients.forEach((clientData) => {
      stats.users.add(clientData.userId);
      stats.sessions.add(clientData.sessionId);
    });

    return {
      ...stats,
      uniqueUsers: stats.users.size,
      uniqueSessions: stats.sessions.size,
    };
  }

  // Obtener sesiones activas
  getActiveSessions() {
    const sessions = new Map<string, { userId: number; socketId: string; connectedAt: Date }>();
    
    this.connectedClients.forEach((clientData, socketId) => {
      sessions.set(clientData.sessionId, {
        userId: clientData.userId,
        socketId,
        connectedAt: new Date(),
      });
    });

    return Array.from(sessions.entries()).map(([sessionId, data]) => ({
      sessionId,
      ...data,
    }));
  }
} 