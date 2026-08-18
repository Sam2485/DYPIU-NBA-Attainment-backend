package com.dypiu.nba.security;

import com.dypiu.nba.controller.CustomErrorController;
import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.GlobalExceptionHandler;
import com.dypiu.nba.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BackendResponseAndErrorHardeningTest {

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @Autowired
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private CustomErrorController customErrorController;

    @Test
    @DisplayName("Test 1: ResourceNotFoundException returns 404 with structured JSON")
    void testResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("School not found with id: sch-test");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleNotFound(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("School not found with id: sch-test", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Test 2: EntityNotFoundException and NoSuchElementException return 404 with structured JSON")
    void testEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Entity does not exist");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleEntityNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Entity does not exist", response.getBody().getMessage());

        NoSuchElementException ex2 = new NoSuchElementException("Element absent");
        ResponseEntity<ApiResponse<Object>> response2 = exceptionHandler.handleEntityNotFound(ex2);
        assertEquals(HttpStatus.NOT_FOUND, response2.getStatusCode());
        assertFalse(response2.getBody().isSuccess());
    }

    @Test
    @DisplayName("Test 3: BadRequestException and IllegalArgumentException return 400 with structured JSON")
    void testBadRequestAndIllegalArgument() {
        BadRequestException ex = new BadRequestException("Invalid parameter value");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleBadRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid parameter value", response.getBody().getMessage());

        IllegalArgumentException ex2 = new IllegalArgumentException("Illegal argument passed");
        ResponseEntity<ApiResponse<Object>> response2 = exceptionHandler.handleIllegalArgument(ex2);
        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
        assertFalse(response2.getBody().isSuccess());
    }

    @Test
    @DisplayName("Test 4: AccessDeniedException returns 403 with structured JSON")
    void testAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied: You do not have permission");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Test 5: AuthenticationException returns 401 with structured JSON")
    void testAuthenticationException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid username or password", response.getBody().getMessage());

        AuthenticationException ex2 = new AuthenticationException("Token expired") {};
        ResponseEntity<ApiResponse<Object>> response2 = exceptionHandler.handleAuthenticationException(ex2);
        assertEquals(HttpStatus.UNAUTHORIZED, response2.getStatusCode());
        assertFalse(response2.getBody().isSuccess());
    }

    @Test
    @DisplayName("Test 6: MethodArgumentNotValidException returns 400 with field errors map")
    void testValidationException() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "requestDto");
        bindingResult.addError(new FieldError("requestDto", "name", "Name is mandatory"));
        bindingResult.addError(new FieldError("requestDto", "email", "Email format is invalid"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ApiResponse<Map<String, String>>> response = exceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
        assertEquals("Name is mandatory", response.getBody().getData().get("name"));
        assertEquals("Email format is invalid", response.getBody().getData().get("email"));
    }

    @Test
    @DisplayName("Test 7: HttpMessageNotReadableException returns 400 without leaking stack traces")
    void testMalformedJsonException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error: Unexpected character");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Malformed JSON request or unreadable request body", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Test 8: DataIntegrityViolationException returns 409/400 without leaking raw SQL or schema internals")
    void testDataIntegrityViolationException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [insert into users (email) values (?)]; constraint [users_email_key]");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertFalse(response.getBody().getMessage().contains("SQL"));
        assertFalse(response.getBody().getMessage().contains("users_email_key"));
        assertEquals("Database constraint violation or duplicate record error.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Test 9: Generic Exception returns 500 with safe internal error message")
    void testGenericException() {
        Exception ex = new NullPointerException("Null reference at com.dypiu.internal.SecretMethod");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertFalse(response.getBody().getMessage().contains("SecretMethod"));
        assertEquals("An unexpected internal error occurred. Please contact administrator.", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Test 10: ResponseStatusException preserves status code and message")
    void testResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden access to school");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleResponseStatusException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Forbidden access to school", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Test 11: HttpRequestMethodNotSupportedException returns 405 Method Not Allowed")
    void testMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("POST"));
    }

    @Test
    @DisplayName("Test 12: HttpMediaTypeNotSupportedException returns 415 Unsupported Media Type")
    void testMediaTypeNotSupportedException() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("application/xml");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleMediaTypeExceptions(ex);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    @DisplayName("Test 13: JwtAuthenticationEntryPoint writes JSON 401 to response")
    void testJwtAuthenticationEntryPoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException authEx = new BadCredentialsException("Unauthenticated request");

        authenticationEntryPoint.commence(request, response, authEx);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Unauthorized access"));
    }

    @Test
    @DisplayName("Test 14: JwtAccessDeniedHandler writes JSON 403 to response")
    void testJwtAccessDeniedHandler() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException accessEx = new AccessDeniedException("Access denied");

        accessDeniedHandler.handle(request, response, accessEx);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Access denied"));
    }

    @Test
    @DisplayName("Test 15: CustomErrorController returns JSON ApiResponse for unmapped 404 or 500 error routes")
    void testCustomErrorController() {
        MockHttpServletRequest request404 = new MockHttpServletRequest();
        request404.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        ResponseEntity<ApiResponse<Object>> response404 = customErrorController.handleError(request404);

        assertEquals(HttpStatus.NOT_FOUND, response404.getStatusCode());
        assertFalse(response404.getBody().isSuccess());
        assertEquals("Endpoint not found", response404.getBody().getMessage());

        MockHttpServletRequest request500 = new MockHttpServletRequest();
        request500.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request500.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Unexpected servlet error");
        ResponseEntity<ApiResponse<Object>> response500 = customErrorController.handleError(request500);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response500.getStatusCode());
        assertFalse(response500.getBody().isSuccess());
        assertEquals("Unexpected servlet error", response500.getBody().getMessage());
    }
}
