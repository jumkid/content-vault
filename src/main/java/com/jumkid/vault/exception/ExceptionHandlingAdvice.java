package com.jumkid.vault.exception;

import com.jumkid.share.controller.response.CustomErrorResponse;
import com.jumkid.share.security.exception.UserProfileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class ExceptionHandlingAdvice {
    @ExceptionHandler({UserProfileNotFoundException.class, AccessDeniedException.class})
    @ResponseStatus(FORBIDDEN)
    public CustomErrorResponse handleUserProfileNotFound(RuntimeException ex) {
        return new CustomErrorResponse(Calendar.getInstance().getTime(), ex.getMessage());
    }

    @ExceptionHandler({FileNotFoundException.class, FileNotAvailableException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CustomErrorResponse handleFileException(Exception ex) {
        log.info(ex.getMessage());
        return new CustomErrorResponse(Calendar.getInstance().getTime(), ex.getMessage());
    }

    @ExceptionHandler({FileStoreServiceException.class})
    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    public CustomErrorResponse handle(FileStoreServiceException ex) {
        log.info("File storage service encounter something wrong {}.", ex.getMessage());
        return new CustomErrorResponse(Calendar.getInstance().getTime(), ex.getMessage());
    }

    @ExceptionHandler({InvalidFieldException.class, HttpRequestMethodNotSupportedException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleBadRequest(RuntimeException e) {
        return new CustomErrorResponse(Calendar.getInstance().getTime(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public CustomErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("The provided argument is missing or invalid.", ex);
        return CustomErrorResponse.builder()
                .timestamp(Calendar.getInstance().getTime())
                .property(ex.getFieldErrors().stream().map(FieldError::getField).toList())
                .details(ex.getFieldErrors().stream().map(FieldError::getDefaultMessage).toList())
                .build();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(BAD_REQUEST)
    public CustomErrorResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter in the request.", ex);
        return CustomErrorResponse.builder()
                .timestamp(Calendar.getInstance().getTime())
                .property(List.of(ex.getParameterName()))
                .details(List.of(Objects.requireNonNull(ex.getMessage())))
                .build();
    }

    @ExceptionHandler(ConversionFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handle(ConversionFailedException ex) {
        log.info("Failed to convert value {}", ex.getMessage());
        return new CustomErrorResponse(Calendar.getInstance().getTime(), ex.getMessage());
    }

    @ExceptionHandler(GalleryNotEmptyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CustomErrorResponse handle(GalleryNotEmptyException ex) {
        return new CustomErrorResponse(Calendar.getInstance().getTime(), ex.getMessage());
    }
}
