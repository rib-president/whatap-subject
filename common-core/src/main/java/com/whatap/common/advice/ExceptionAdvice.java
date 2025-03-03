package com.whatap.common.advice;

import com.whatap.common.dto.ErrorItemResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

@RestControllerAdvice
public class ExceptionAdvice {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<List<ErrorItemResponseDto>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
    ex.printStackTrace();

    ErrorItemResponseDto errors = ErrorItemResponseDto.builder()
        .code(HttpStatus.BAD_REQUEST.name())
        .message(ex.getMessage())
        .build();

    return new ResponseEntity<>(List.of(errors), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  protected ResponseEntity<List<ErrorItemResponseDto>> handleMethodArgumentNotValidException(HttpMessageNotReadableException ex, WebRequest request) {
    ex.printStackTrace();

    ErrorItemResponseDto errors = ErrorItemResponseDto.builder()
        .code(HttpStatus.BAD_REQUEST.name())
        .message(ex.getMessage())
        .build();

    return new ResponseEntity<>(List.of(errors), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(RuntimeException.class)
  protected ResponseEntity<List<ErrorItemResponseDto>> handleRuntimeException(RuntimeException ex, WebRequest request) {
    ex.printStackTrace();

    ErrorItemResponseDto errors = ErrorItemResponseDto.builder()
        .code(HttpStatus.INTERNAL_SERVER_ERROR.name())
        .message(ex.getMessage())
        .build();

    return new ResponseEntity<>(List.of(errors), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<List<ErrorItemResponseDto>> handleException(Exception ex, WebRequest request) {
    ex.printStackTrace();

    ErrorItemResponseDto errors = ErrorItemResponseDto.builder()
        .code(HttpStatus.UNPROCESSABLE_ENTITY.name())
        .message(ex.getMessage())
        .build();

    return new ResponseEntity<>(List.of(errors), HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
