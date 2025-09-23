import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // CORS configuration
  app.enableCors({
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
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'Accept', 'Origin', 'X-Requested-With'],
  });

  // Global API prefix
  app.setGlobalPrefix('api/v1');

  // Global validation
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  // Swagger configuration
  const config = new DocumentBuilder()
    .setTitle('YEGO Integral API')
    .setDescription('API del Sistema YEGO Integral')
    .setVersion('1.0')
    .addBearerAuth()
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api/docs', app, document);

  // Application port
  const port = process.env.PORT || 3001;
  
  await app.listen(port);
  console.log(`🚀 Server running on http://localhost:${port}`);
  console.log(`📚 Documentation at http://localhost:${port}/api/docs`);
}

bootstrap().catch((error) => {
  console.error('Error starting application:', error);
  process.exit(1);
}); 