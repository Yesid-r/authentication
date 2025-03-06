# Spring Boot JWT Authentication Service with Email Verification

A robust Spring Boot authentication service that provides secure user registration with email verification, JWT-based authentication, and password management capabilities. The service includes caching for OTP management and Docker integration for easy deployment.

This service implements a complete authentication flow with email verification using one-time passwords (OTP) and JWT tokens for secure session management. It provides a RESTful API with comprehensive validation, error handling, and security measures. The service uses PostgreSQL for data persistence and includes Caffeine caching for improved performance.

## Repository Structure
```
.
├── docker-compose.yml              # Docker configuration for PostgreSQL database
├── pom.xml                        # Maven project configuration and dependencies
└── src/
    └── main/
        ├── java/com/example/authentication/
        │   ├── AuthenticationApplication.java    # Main application entry point
        │   ├── configuration/                    # Application configurations
        │   │   ├── AppConfiguration.java         # Core authentication config
        │   │   ├── CaffeineCacheConfig.java     # Cache configuration
        │   │   └── MailConfiguration.java        # Email service config
        │   ├── controller/                       # REST API endpoints
        │   │   ├── AuthenticationController.java # Authentication endpoints
        │   │   └── ProfileController.java        # User profile endpoints
        │   ├── security/                         # Security configurations
        │   │   ├── JwtAuthenticationFilter.java  # JWT token validation
        │   │   └── SecurityConfiguration.java    # Spring Security setup
        │   └── service/                          # Business logic layer
        │       ├── AuthenticationService.java    # Authentication operations
        │       ├── EmailService.java            # Email sending functionality
        │       └── JwtService.java              # JWT token management
        └── resources/
            ├── application.properties           # Application configuration
            └── templates/
                └── otp-sender.html             # Email template for OTP
```

## Usage Instructions
### Prerequisites
- Java 17 or higher
- Docker and Docker Compose
- PostgreSQL (or use provided Docker container)
- SMTP server access for email sending

### Installation
1. Clone the repository:
```bash
git clone https://github.com/Yesid-r/authentication.git
cd authentication
```

2. Start the PostgreSQL database using Docker:
```bash
docker-compose up -d
```

3. Build the application:
```bash
./mvnw clean install
```

### Quick Start
1. Start the application:
```bash
./mvnw spring-boot:run
```

2. Register a new user:
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "gender": "MALE",
    "role": "USER"
  }'
```

3. Verify email using OTP:
```bash
curl -X POST http://localhost:8080/api/v1/auth/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "otp": "123456"
  }'
```

### More Detailed Examples
1. Login and receive JWT token:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "Password123!"
  }'
```

2. Reset password flow:
```bash
# Request password reset
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com"
  }'

# Reset password with OTP
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "NewPassword123!",
    "confirmPassword": "NewPassword123!",
    "otp": "123456"
  }'
```

### Troubleshooting
1. Email Verification Issues
- Problem: OTP not received
  - Check spam folder
  - Verify email configuration in application.properties
  - Enable debug logging: `logging.level.com.example.authentication=DEBUG`
  - Check email service logs for delivery status

2. Database Connection Issues
- Problem: Cannot connect to PostgreSQL
  - Verify Docker container is running: `docker ps`
  - Check database credentials in application.properties
  - Ensure port 5432 is available
  - Database logs: `docker logs authentication_pg_sql`

3. JWT Token Issues
- Problem: Invalid token
  - Check token expiration
  - Verify token format in Authorization header
  - Enable security debug: `logging.level.org.springframework.security=DEBUG`

## Data Flow
The authentication service follows a layered architecture for processing authentication requests.

```ascii
Client -> Controller -> Service -> Repository
   ^          |            |           |
   |          v            v           v
   +---- JWT Token     Cache DB    PostgreSQL
```

Component interactions:
1. Controllers receive HTTP requests and validate input data
2. Services implement business logic and manage transactions
3. JWT filter validates tokens for protected endpoints
4. Email service sends verification OTPs asynchronously
5. Caffeine cache stores OTPs with 2-minute expiration
6. PostgreSQL stores user data and credentials
7. Security layer manages authentication and authorization

## Infrastructure

![Infrastructure diagram](./docs/infra.png)
### Docker Resources
- PostgreSQL Container:
  - Name: authentication_pg_sql
  - Image: postgres:latest
  - Ports: 5432:5432
  - Environment:
    - POSTGRES_USER: root
    - POSTGRES_PASSWORD: root
    - PGDATA: /data/postgres
  - Volume: postgres:/data/postgres
  - Network: authentication-net (bridge)

### Cache Configuration
- Provider: Caffeine
- Cache Name: "user"
- Initial Capacity: 10
- Maximum Size: 500
- Expiration: 2 minutes after write
- Statistics: Enabled