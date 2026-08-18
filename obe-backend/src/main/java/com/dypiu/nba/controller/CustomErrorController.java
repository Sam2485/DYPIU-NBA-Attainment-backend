package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        if (status != null) {
            try {
                int statusCode = Integer.parseInt(status.toString());
                httpStatus = HttpStatus.valueOf(statusCode);
            } catch (Exception ignored) {}
        }

        String msg = (message != null && !message.toString().isBlank())
                ? message.toString()
                : (httpStatus == HttpStatus.NOT_FOUND ? "Endpoint not found" : "Error processing request");

        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.builder()
                        .success(false)
                        .message(msg)
                        .data(null)
                        .build());
    }
}
