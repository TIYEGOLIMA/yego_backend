import { Module } from '@nestjs/common';
import { WebsocketGateway } from './websocket.gateway';
import { WsJwtGuard } from '../auth/guards/ws-jwt.guard';
import { JwtModule } from '@nestjs/jwt';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { getJwtConfig } from '../../config/jwt.config';

@Module({
  imports: [
    JwtModule.registerAsync({
      imports: [ConfigModule],
      useFactory: (configService: ConfigService) => getJwtConfig(configService),
      inject: [ConfigService],
    }),
  ],
  providers: [WebsocketGateway, WsJwtGuard],
  exports: [WebsocketGateway],
})
export class WebsocketModule {} 