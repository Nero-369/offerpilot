package com.offerpilot.api;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.BadCredentialsException;
import java.time.Instant;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class) ResponseEntity<?> unauthorized(BadCredentialsException ex){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("timestamp",Instant.now(),"error","用户名或密码错误"));}
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> badRequest(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("timestamp",Instant.now(),"error",ex.getMessage()));}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException ex){return ResponseEntity.badRequest().body(Map.of("timestamp",Instant.now(),"error","Validation failed","fields",ex.getBindingResult().getFieldErrors().stream().collect(java.util.stream.Collectors.toMap(e->e.getField(),e->e.getDefaultMessage()==null?"invalid":e.getDefaultMessage(),(a,b)->a))));}
}
