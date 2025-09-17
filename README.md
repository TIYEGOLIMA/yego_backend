# YEGO Integral API

API del Sistema YEGO Integral construida con NestJS y TypeScript.

## Descripción

Sistema empresarial completo para la gestión integral de YEGO, incluyendo:
- Gestión de usuarios y roles con permisos dinámicos
- Sistema de autenticación JWT con refresh tokens
- WebSockets para comunicación en tiempo real
- Base de datos PostgreSQL con auditoría completa
- Sistema de sesiones activas
- Configuraciones dinámicas del sistema
- Documentación automática con Swagger

## Tecnologías

- **Framework**: NestJS
- **Lenguaje**: TypeScript
- **Base de datos**: PostgreSQL
- **ORM**: TypeORM
- **Autenticación**: JWT + Passport
- **WebSockets**: Socket.IO
- **Documentación**: Swagger/OpenAPI
- **Validación**: class-validator
- **Testing**: Jest + Supertest
- **Seguridad**: bcrypt, rate limiting, CORS

## Instalación

1. Clonar el repositorio
2. Instalar dependencias:
   ```bash
   npm install
   ```

3. Configurar variables de entorno:
   ```bash
   cp env.example .env
   # Editar .env con tus configuraciones
   ```

4. Configurar base de datos PostgreSQL:
   - Crear base de datos `yego_integral`
   - Ejecutar el script `database/schema.sql`

5. Inicializar el sistema:
   ```bash
   npm run init:system
   ```

6. Iniciar el servidor:
   ```bash
   # Desarrollo
   npm run start:dev
   
   # Producción
   npm run start:prod
   ```

## Estructura del Proyecto

```
src/
├── config/                    # Configuraciones
├── modules/                   # Módulos de la aplicación
│   ├── auth/                  # Autenticación y autorización
│   ├── users/                 # Gestión de usuarios
│   ├── roles/                 # Gestión de roles y permisos
│   ├── sessions/              # Gestión de sesiones activas
│   ├── audit/                 # Logs de auditoría
│   ├── configuration/         # Configuraciones del sistema
│   └── websocket/             # WebSockets y comunicación en tiempo real
├── scripts/                   # Scripts de inicialización
└── main.ts                   # Punto de entrada
```

## API Endpoints

### Autenticación
- `POST /api/v1/auth/login` - Iniciar sesión
- `POST /api/v1/auth/register` - Registrar usuario
- `POST /api/v1/auth/logout` - Cerrar sesión
- `POST /api/v1/auth/refresh` - Renovar token
- `POST /api/v1/auth/create-superadmin` - Crear superadmin
- `POST /api/v1/auth/create-test-user` - Crear usuario de prueba

### Usuarios
- `GET /api/v1/users` - Listar usuarios (paginado)
- `POST /api/v1/users` - Crear usuario
- `GET /api/v1/users/:id` - Obtener usuario
- `PUT /api/v1/users/:id` - Actualizar usuario
- `DELETE /api/v1/users/:id` - Eliminar usuario (soft delete)
- `POST /api/v1/users/:id/change-password` - Cambiar contraseña

### Roles
- `GET /api/v1/roles` - Listar roles
- `POST /api/v1/roles` - Crear rol
- `GET /api/v1/roles/:id` - Obtener rol
- `PUT /api/v1/roles/:id` - Actualizar rol
- `DELETE /api/v1/roles/:id` - Eliminar rol
- `POST /api/v1/roles/initialize` - Inicializar roles por defecto

### Sesiones
- `GET /api/v1/sessions` - Listar sesiones activas
- `GET /api/v1/sessions/stats` - Estadísticas de sesiones
- `GET /api/v1/sessions/websocket/stats` - Estadísticas WebSocket
- `GET /api/v1/sessions/websocket/sessions` - Sesiones WebSocket activas
- `DELETE /api/v1/sessions/:id` - Cerrar sesión específica
- `DELETE /api/v1/sessions/user/:userId` - Cerrar todas las sesiones de un usuario
- `POST /api/v1/sessions/cleanup` - Limpiar sesiones expiradas

### Auditoría
- `GET /api/v1/audit` - Listar logs de auditoría (con filtros)
- `GET /api/v1/audit/stats` - Estadísticas de auditoría
- `GET /api/v1/audit/recent` - Actividad reciente
- `GET /api/v1/audit/user/:userId` - Logs de un usuario específico
- `GET /api/v1/audit/action/:action` - Logs por acción
- `GET /api/v1/audit/resource/:resource` - Logs por recurso

### Configuración
- `GET /api/v1/configuration` - Listar todas las configuraciones
- `GET /api/v1/configuration/system` - Configuración del sistema por categorías
- `GET /api/v1/configuration/categories` - Categorías de configuración
- `GET /api/v1/configuration/category/:category` - Configuraciones por categoría
- `GET /api/v1/configuration/:key` - Obtener configuración específica
- `PUT /api/v1/configuration/:key` - Actualizar configuración
- `POST /api/v1/configuration/:key` - Establecer valor de configuración
- `DELETE /api/v1/configuration/:key` - Eliminar configuración
- `POST /api/v1/configuration/initialize` - Inicializar configuraciones por defecto

## WebSockets

### Eventos Disponibles
- `connection-established` - Conexión establecida
- `session-registered` - Sesión registrada
- `session-closed` - Sesión cerrada
- `force-logout` - Logout forzado
- `ping/pong` - Heartbeat

### Métodos del Servicio
- `emitToUser(userId, event, data)` - Emitir a usuario específico
- `emitToSession(sessionId, event, data)` - Emitir a sesión específica
- `emitToAll(event, data)` - Emitir a todos
- `closeSession(sessionId, reason)` - Cerrar sesión
- `forceLogout(userId, reason)` - Forzar logout

## Documentación

La documentación de la API está disponible en:
- **Swagger UI**: `http://localhost:3001/api/docs`
- **OpenAPI JSON**: `http://localhost:3001/api/docs-json`

## Testing

```bash
# Ejecutar tests unitarios
npm run test

# Ejecutar tests con coverage
npm run test:cov

# Ejecutar tests e2e
npm run test:e2e

# Ejecutar tests en modo watch
npm run test:watch
```

## Variables de Entorno

### Requeridas
- `DB_HOST` - Host de la base de datos
- `DB_PORT` - Puerto de la base de datos
- `DB_USER` - Usuario de la base de datos
- `DB_PASSWORD` - Contraseña de la base de datos
- `DB_NAME` - Nombre de la base de datos
- `JWT_SECRET` - Secreto para JWT

### Opcionales
- `PORT` - Puerto del servidor (default: 3001)
- `NODE_ENV` - Entorno (development/production)
- `FRONTEND_URL` - URL del frontend para CORS
- `SOCKET_PORT` - Puerto para WebSockets (default: 3010)
- `ENABLE_AUDIT_LOGS` - Habilitar logs de auditoría
- `SESSION_TIMEOUT` - Tiempo de sesión en segundos

## Scripts Disponibles

- `npm run start:dev` - Iniciar en modo desarrollo
- `npm run start:prod` - Iniciar en modo producción
- `npm run build` - Construir la aplicación
- `npm run test` - Ejecutar tests
- `npm run lint` - Ejecutar linter
- `npm run format` - Formatear código
- `npm run init:system` - Inicializar sistema con datos por defecto

## Base de Datos

### Configuración
- **Tipo**: PostgreSQL
- **Nombre**: `yego_integral`
- **Puerto**: 5432 (por defecto)

### Tablas Principales
- `users` - Usuarios del sistema
- `roles` - Roles y permisos
- `user_roles` - Relación usuarios-roles
- `sessions` - Sesiones activas
- `audit_logs` - Logs de auditoría
- `configurations` - Configuraciones del sistema
- `modules` - Módulos del sistema
- `imports` - Registro de importaciones
- `password_resets` - Reset de contraseñas
- `notifications` - Notificaciones

### Vistas Útiles
- `v_users_with_roles` - Usuarios con sus roles
- `v_active_sessions` - Sesiones activas
- `v_audit_stats` - Estadísticas de auditoría

## Seguridad

- Autenticación JWT con refresh tokens
- Contraseñas hasheadas con bcrypt (12 rounds)
- Validación de datos con class-validator
- CORS configurado
- Rate limiting configurable
- Logs de auditoría completos
- WebSockets con autenticación JWT
- Control de sesiones activas
- Soft delete para usuarios

## Roles y Permisos

### Roles por Defecto
- **superadmin**: Acceso total al sistema
- **admin**: Administrador con permisos amplios
- **supervisor**: Supervisor con permisos limitados
- **operador**: Operador básico
- **conductor**: Conductor con acceso mínimo

### Sistema de Permisos
- Permisos granulares por módulo y acción
- Verificación de permisos en tiempo real
- Roles dinámicos configurables
- Middleware de autorización automático

## Desarrollo

### Estructura de Módulos
Cada módulo sigue la estructura:
```
module/
├── dto/              # Data Transfer Objects
├── entities/         # Entidades de TypeORM
├── guards/           # Guards de autenticación/autorización
├── strategies/       # Estrategias de Passport
├── module.controller.ts
├── module.service.ts
└── module.module.ts
```

### Agregar Nuevo Módulo
1. Crear carpeta del módulo en `src/modules/`
2. Implementar entidad, DTO, servicio y controlador
3. Registrar el módulo en `app.module.ts`
4. Agregar entidad a la configuración de TypeORM
5. Configurar permisos en el sistema de roles

## Producción

### Variables de Entorno
```bash
NODE_ENV=production
PORT=3001
DB_HOST=your-db-host
DB_USER=your-db-user
DB_PASSWORD=your-secure-password
DB_NAME=yego_integral
JWT_SECRET=your-super-secure-jwt-secret
ENABLE_AUDIT_LOGS=true
SESSION_TIMEOUT=3600
```

### Optimizaciones
- Habilitar compresión
- Configurar rate limiting
- Usar HTTPS
- Configurar logs de producción
- Monitoreo y métricas
- Cache de configuraciones
- Limpieza automática de sesiones expiradas

## Credenciales por Defecto

Después de ejecutar `npm run init:system`:

- **Usuario**: `superadmin`
- **Contraseña**: `superadmin123`

⚠️ **IMPORTANTE**: Cambia la contraseña del superadmin después del primer login.

## Licencia

MIT