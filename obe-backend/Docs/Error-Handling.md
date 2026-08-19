# Error Handling & Standard Error Response Schema

## Centralized Exception Handler

All API exceptions are intercepted globally by `@RestControllerAdvice` in `com.dypiu.nba.exception.GlobalExceptionHandler`.

### Response Format Schema

```json
{
  "success": false,
  "message": "Validation failed / Resource not found",
  "data": {
    "fieldName": "Specific error description"
  }
}
```

### HTTP Status Mapping

| Exception Class | HTTP Status Code | Description |
| :--- | :--- | :--- |
| `ResourceNotFoundException` | `404 NOT FOUND` | Entity or resource ID does not exist. |
| `BadRequestException` | `400 BAD REQUEST` | Business rule or request parameter invalid. |
| `MethodArgumentNotValidException` | `400 BAD REQUEST` | Validation annotation failure (`@NotBlank`, `@Email`). |
| `BadCredentialsException` | `401 UNAUTHORIZED` | Username or password incorrect. |
| `AccessDeniedException` | `403 FORBIDDEN` | Insufficient role permissions. |
| `Exception` | `500 INTERNAL SERVER ERROR` | Unhandled runtime exception. Stack traces hidden from client. |
