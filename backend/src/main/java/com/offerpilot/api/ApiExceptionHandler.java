package com.offerpilot.api;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> badRequest(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("timestamp",Instant.now(),"error",ex.getMessage()));}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException ex){return ResponseEntity.badRequest().body(Map.of("timestamp",Instant.now(),"error","Validation failed","fields",ex.getBindingResult().getFieldErrors().stream().collect(java.util.stream.Collectors.toMap(e->e.getField(),e->e.getDefaultMessage()==null?"invalid":e.getDefaultMessage(),(a,b)->a))));}
}
