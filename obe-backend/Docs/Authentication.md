# Authentication Architecture

## JWT Authentication Flow

The backend uses stateless **JSON Web Tokens (JWT)** generated via HMAC SHA-256 signatures (`jjwt-api 0.12.6`).

```text
[ Client (Browser) ]                     [ Spring Security ]
         │                                       │
         ├────── POST /api/v1/auth/login ───────►│
         │   { username, password }              │ (Verifies BCrypt Hash)
         │                                       │
         │◄───── 200 OK + JWT Token ─────────────┤
         │   { token, userProfile }              │
         │                                       │
         ├────── Request with Bearer Token ─────►│
         │   Header: Authorization: Bearer <jwt> │ (JwtAuthenticationFilter validates token)
         │                                       │
```

## Security Credentials & Token Lifespan
- **Algorithm**: HMAC-SHA256 (`Keys.hmacShaKeyFor`)
- **Default Expiration**: 24 Hours (`86400000 ms`)
- **Password Hashing**: BCrypt Password Encoder (`BCryptPasswordEncoder` with strength 10). Plaintext passwords are never logged or stored.
