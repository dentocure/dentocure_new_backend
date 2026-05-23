# JWT Authentication & Authorization Guide

## Overview
This application now uses JWT (JSON Web Token) based authentication with role-based access control (RBAC). All API endpoints (except login/refresh) require a valid JWT token in the `Authorization` header.

## Available Roles
- **ADMIN** - Full access to all endpoints
- **DOCTOR** - Access to doctor-specific endpoints  
- **RECEPTIONIST** - Access to appointment and patient management endpoints

## Sample Users (Pre-configured)

### Admin User
- **Username**: `admin`
- **Password**: `admin123`
- **Role**: ADMIN
- **Email**: admin@dentocure.com

### Doctor User
- **Username**: `doctor`
- **Password**: `doctor123`
- **Role**: DOCTOR
- **Email**: doctor@dentocure.com

### Receptionist User
- **Username**: `receptionist`
- **Password**: `receptionist123`
- **Role**: RECEPTIONIST
- **Email**: receptionist@dentocure.com

## Authentication Endpoints

### 1. Login
**POST** `/api/auth/login`

Request:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": "uuid-here",
  "username": "admin",
  "role": "ADMIN"
}
```

### 2. Refresh Token
**POST** `/api/auth/refresh`

Request:
```json
{
  "refreshToken": "your-refresh-token-here"
}
```

Response:
```json
{
  "accessToken": "new-access-token",
  "refreshToken": "refresh-token",
  "userId": "uuid",
  "username": "admin",
  "role": "ADMIN"
}
```

### 3. Get Current User Profile
**GET** `/api/auth/me`

Headers:
```
Authorization: Bearer {accessToken}
```

Response:
```json
{
  "id": "user-id",
  "username": "admin",
  "email": "admin@dentocure.com",
  "role": "ADMIN",
  "active": true,
  "createdAt": "2024-01-15T10:30:00",
  "lastLogin": "2024-01-15T10:35:00"
}
```

### 4. Change Password
**PUT** `/api/auth/change-password`

Headers:
```
Authorization: Bearer {accessToken}
```

Request:
```json
{
  "oldPassword": "admin123",
  "newPassword": "newPassword456"
}
```

Response:
```json
{
  "message": "Password changed successfully"
}
```

### 5. Logout
**POST** `/api/auth/logout`

Response:
```json
{
  "message": "Logged out successfully. Please discard the tokens."
}
```

## Token Details

### Access Token
- **Duration**: 15 minutes (900,000 ms)
- **Use Case**: Used in every API request
- **Storage**: Send in `Authorization: Bearer {token}` header

### Refresh Token
- **Duration**: 7 days (604,800,000 ms)
- **Use Case**: Used to obtain a new access token when current one expires
- **Storage**: Securely store (HttpOnly cookie recommended in production)

## How to Use Tokens

### Making Protected API Requests

Include the access token in the Authorization header:

```bash
curl -X GET http://localhost:8080/api/doctors \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

### When Token Expires

1. You'll receive a 401 Unauthorized response
2. Use the refresh token to get a new access token
3. POST to `/api/auth/refresh` with your refresh token
4. Store the new access token and continue

## Brute-Force Protection

The authentication system includes brute-force protection:
- **Max Login Attempts**: 5
- **Lockout Duration**: 15 minutes
- **Behavior**: Account gets locked after 5 failed attempts for 15 minutes

## Security Configuration

### JWT Secret
The JWT secret is configured in `application.properties`:
```properties
jwt.secret=your-super-secret-key-that-should-be-at-least-64-characters...
jwt.access-token-expiration=900000
jwt.refresh-token-expiration=604800000
```

⚠️ **Production**: Change the `jwt.secret` to a long, random string (at least 64 characters). Generate one using:
```bash
openssl rand -base64 64
```

### Password Hashing
- **Algorithm**: BCrypt
- **Cost Factor**: 12 (high security)
- All passwords are automatically hashed before storage

### CORS Configuration
Allowed origins (configurable in SecurityConfig):
- http://localhost:3000 (React app)
- http://localhost:5173 (Vite dev server)

## Protected Endpoints (Require JWT)

All API endpoints except `/api/auth/login` and `/api/auth/refresh` are now protected:

- ✅ Public: `POST /api/auth/login`
- ✅ Public: `POST /api/auth/refresh`
- 🔒 Protected: `GET /api/auth/me`
- 🔒 Protected: `PUT /api/auth/change-password`
- 🔒 Protected: `POST /api/auth/logout`
- 🔒 Protected: `GET /api/doctors`
- 🔒 Protected: `GET /api/patients`
- 🔒 Protected: `GET /api/appointments`
- 🔒 Protected: All other endpoints...

## Role-Based Access Control

You can add role restrictions to endpoints using `@PreAuthorize` annotation:

```java
@GetMapping
@PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
    // Only ADMIN and DOCTOR roles can access
}

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteDoctor(@PathVariable String id) {
    // Only ADMIN can delete
}
```

## Common Issues

### 1. Invalid Signature or Token
- Token is malformed or tampered with
- Ensure you're sending the exact token without modifications

### 2. Token Expired
- Access token has expired (15 min)
- Use the refresh token to get a new one

### 3. Unauthorized (401)
- No token provided in header
- Token is invalid or expired
- User doesn't have required role

### 4. Account Locked
- Too many failed login attempts
- Account will unlock after 15 minutes

## Next Steps

1. ✅ JWT authentication is set up
2. ✅ Sample users are created at startup
3. ✅ All endpoints are protected
4. ⏳ Next: Add `@PreAuthorize` annotations to specific endpoints for granular RBAC
5. ⏳ Next: Implement user management endpoints (admin only)
6. ⏳ Next: Add audit logging for authentication events
