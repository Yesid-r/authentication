# Spring Boot JWT Authentication Service with Email Verification

A robust Spring Boot authentication service that provides secure user registration with email verification, JWT-based authentication, and password management capabilities. The service includes caching for OTP management and Docker integration for easy deployment.

This service implements a complete authentication flow with email verification using one-time passwords (OTP) and JWT tokens for secure session management. It provides a RESTful API with comprehensive validation, error handling, and security measures. The service uses PostgreSQL for data persistence and includes Caffeine caching for improved performance.

## Repository Structure
```
.
├── docker-compose.yml              # Docker configuration for the entire application
├── Dockerfile                      # Container definition for the authentication service
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

## Quick Start Guide

### Prerequisites
- Docker and Docker Compose

### Installation and Deployment
1. Clone the repository:
```bash
git clone https://github.com/Yesid-r/authentication.git
cd authentication
```

2. Start the application using Docker Compose:
```bash
docker-compose up -d
```

That's it! The application is now running at [http://localhost:8080](http://localhost:8080) with all necessary dependencies including the PostgreSQL database.

## API Documentation

This project includes comprehensive API documentation powered by Swagger UI.
To explore and interact with all available endpoints:

1. Once the application is running
2. Navigate to: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
3. From there, you can browse all available endpoints, test API calls, and understand request/response formats

The Swagger UI provides a convenient interface for testing the authentication service and understanding all available functionality without needing additional tools.

## Usage Examples

### Registration and Authentication Flow
1. Register a new user:
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

2. Verify email using OTP:
```bash
curl -X POST http://localhost:8080/api/v1/auth/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "otp": "123456"
  }'
```

3. Login and receive JWT token:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "Password123!"
  }'
```

### Password Management
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

## Troubleshooting

### Docker-related Issues
- Problem: Containers not starting
  - Check Docker logs: `docker-compose logs`
  - Verify ports are not in use: `netstat -tuln | grep 8080`
  - Restart Docker service if needed
  - Try rebuilding: `docker-compose down && docker-compose up --build -d`

### Email Verification Issues
- Problem: OTP not received
  - Check spam folder
  - Verify email configuration in application.properties
  - Check container logs: `docker-compose logs authentication-service`
  - Enable debug logging in docker-compose.yml environment variables

### JWT Token Issues
- Problem: Invalid token
  - Check token expiration
  - Verify token format in Authorization header
  - Check container logs for security issues

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
- Authentication Service Container:
  - Built from local Dockerfile
  - Ports: 8080:8080
  - Network: authentication-net
- PostgreSQL Container:
  - Name: authentication_pg_sql
  - Image: postgres:latest
  - Ports: 5432:5432
  - Environment:
    - POSTGRES_USER: root
    - POSTGRES_PASSWORD: root
    - PGDATA: /data/postgres
  - Volume: postgres:/data/postgres
  - Network: authentication-net

### Cache Configuration
- Provider: Caffeine
- Cache Name: "user"
- Initial Capacity: 10
- Maximum Size: 500
- Expiration: 2 minutes after write
- Statistics: Enabled