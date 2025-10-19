# Yego Backend - Spring Boot

Este proyecto es la conversión del backend de Yego de **NestJS/TypeScript** a **Java con Spring Boot**, manteniendo la misma funcionalidad y siguiendo las mejores prácticas de arquitectura de Spring.

## 🏗️ Arquitectura de Paquetes

El proyecto sigue una estructura de paquetes moderna y organizada con separación clara entre DTOs y entidades:

```
com.yego.backend/
├── controller/          # REST Controllers (@RestController)
├── entity/             # Entidades y DTOs organizados
│   ├── api/           # DTOs para API (@Data, validaciones)
│   └── entities/      # Entidades JPA (@Entity)
├── repository/         # Interfaces JpaRepository
├── service/            # Interfaces de servicios
│   └── impl/          # Implementaciones (@Service)
├── config/            # Configuraciones (@Configuration)
├── emitteds/          # Eventos y mensajes (ApplicationEvent)
└── YegoBackendApplication.java
```

### 📁 Organización de Entity

- **`entity/api/`**: Contiene todos los DTOs (Data Transfer Objects) para la API
  - DTOs de request/response (`LoginDto`, `RegisterDto`, etc.)
  - DTOs de WebSocket (`ConnectionStatsDto`, `SessionDataDto`, etc.)
  - Validaciones con Bean Validation (`@NotBlank`, `@Email`, etc.)

- **`entity/entities/`**: Contiene únicamente las entidades JPA
  - Entidades de base de datos (`User`, `Session`)
  - Anotaciones JPA (`@Entity`, `@Table`, `@Column`)
  - Relaciones entre entidades (`@ManyToOne`, `@OneToMany`)

## 🚀 Características Principales

### ✅ Funcionalidades Convertidas

- **Autenticación JWT**: Login, registro, cambio de contraseña
- **WebSocket**: Conexiones en tiempo real con autenticación JWT
- **Gestión de Sesiones**: Tracking de sesiones activas con geolocalización
- **Seguridad**: Filtros JWT, CORS, validación de contraseñas
- **Eventos**: Sistema de eventos para WebSocket y auditoría
- **Base de Datos**: Entidades JPA para PostgreSQL

### 🔧 Tecnologías Utilizadas

- **Spring Boot 2.7.14**
- **Spring Security** (JWT Authentication)
- **Spring Data JPA** (PostgreSQL)
- **Spring WebSocket** (STOMP)
- **Lombok** (Reducir boilerplate)
- **BCrypt** (Hash de contraseñas)
- **JJWT** (JSON Web Tokens)

## 📋 Requisitos Previos

- **Java 11** o superior
- **Maven 3.6+**
- **PostgreSQL 12+**
- **IDE** (IntelliJ IDEA, Eclipse, VS Code)

## ⚙️ Configuración

### 1. Variables de Entorno

Crear archivo `.env` o configurar variables del sistema:

```bash
# Base de datos
DB_USERNAME=postgres
DB_PASSWORD=tu_password
DB_URL=jdbc:postgresql://localhost:5432/yego_db

# JWT
JWT_SECRET=tu-clave-secreta-muy-larga-y-segura-aqui
JWT_EXPIRES_IN=3600

# Frontend URLs
FRONTEND_URL=http://localhost:3000
FRONTEND_DEV_URL=http://localhost:5173

```

### 2. Base de Datos

Crear la base de datos PostgreSQL:

```sql
CREATE DATABASE yego_db;
CREATE USER yego_user WITH PASSWORD 'tu_password';
GRANT ALL PRIVILEGES ON DATABASE yego_db TO yego_user;
```

### 3. Configuración application.yml

El archivo `application.yml` ya está configurado con valores por defecto. Ajustar según el entorno:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/yego_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}

jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: ${JWT_EXPIRES_IN:3600}
```

## 🚀 Ejecución

### Desarrollo

```bash
# Compilar y ejecutar
mvn spring-boot:run

# O usando el wrapper
./mvnw spring-boot:run
```

### Producción

```bash
# Compilar JAR
mvn clean package

# Ejecutar JAR
java -jar target/yego-backend-1.0.0.jar
```

La aplicación estará disponible en: `http://localhost:8080`

## 📡 Endpoints API

### Autenticación

```http
POST /api/auth/login
POST /api/auth/register
POST /api/auth/logout
POST /api/auth/change-password
GET  /api/auth/profile
GET  /api/auth/validate
```

### WebSocket

```
Endpoint: ws://localhost:8080/wss
Protocolo: STOMP

Mensajes:
- /app/register-session
- /app/ping
- /user/queue/pong
- /user/queue/connection-established
```

## 🔄 Mapeo de Conceptos NestJS → Spring Boot

| NestJS | Spring Boot |
|--------|-------------|
| `@Controller` | `@RestController` |
| `@Injectable` | `@Service` |
| `@Entity` (TypeORM) | `@Entity` (JPA) |
| `Repository<T>` | `JpaRepository<T, ID>` |
| `@WebSocketGateway` | `@Controller` + `@MessageMapping` |
| `@UseGuards(JwtAuthGuard)` | `JwtRequestFilter` |
| `EventEmitter` | `ApplicationEventPublisher` |
| `@SubscribeMessage` | `@MessageMapping` |
| `ConfigService` | `@Value` + `application.yml` |

## 🏃‍♂️ Guía de Migración

### 1. Estructura de Archivos

```
# NestJS
src/modules/auth/auth.service.ts
src/modules/auth/dto/login.dto.ts
src/modules/users/entities/user.entity.ts

# Spring Boot
service/AuthService.java
service/impl/AuthServiceImpl.java
entity/api/LoginDto.java
entity/entities/User.java
```

### 2. Decoradores → Anotaciones

```typescript
// NestJS
@Injectable()
export class AuthService {
  @InjectRepository(User)
  private userRepository: Repository<User>
}
```

```java
// Spring Boot
@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
}
```

### 3. WebSocket

```typescript
// NestJS
@WebSocketGateway()
export class WebsocketGateway {
  @SubscribeMessage('ping')
  handlePing() { /* ... */ }
}
```

```java
// Spring Boot
@Controller
public class WebSocketController {
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public PongResponseDto handlePing() { /* ... */ }
}
```

## 🧪 Testing

```bash
# Ejecutar tests
mvn test

# Tests con cobertura
mvn test jacoco:report
```

## 📦 Build y Deploy

```bash
# Build para producción
mvn clean package -Pprod

# Docker (opcional)
docker build -t yego-backend .
docker run -p 8080:8080 yego-backend
```

## 🔍 Monitoreo

Spring Boot Actuator está habilitado:

- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`

## 📝 Notas de Conversión

### Cambios Principales

1. **TypeScript → Java**: Tipado estático nativo
2. **Decoradores → Anotaciones**: `@Injectable` → `@Service`
3. **Promises → CompletableFuture**: Para operaciones asíncronas
4. **EventEmitter → ApplicationEvent**: Sistema de eventos de Spring
5. **Socket.io → STOMP**: Protocolo WebSocket estándar

### Funcionalidades Equivalentes

- ✅ Autenticación JWT
- ✅ WebSocket con autenticación
- ✅ Gestión de sesiones
- ✅ Validación de DTOs
- ✅ Manejo de errores
- ✅ CORS configurado
- ✅ Logging estructurado

## 🤝 Contribución

1. Fork del proyecto
2. Crear rama feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

## 📄 Licencia

Este proyecto está bajo la licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

---

**Desarrollado con ❤️ usando Spring Boot**