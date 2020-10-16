package com.jumkid.vault.controller;

import com.jumkid.share.controller.response.CustomErrorResponse;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.exception.InvalidFieldException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Calendar;

import org.springframework.http.HttpStatus;

@RestControllerAdvice
@Slf4j
public class AdviceController {

    @ExceptionHandler({FileNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CustomErrorResponse handle(FileNotFoundException ex) {
        log.info("File could not be found {}", ex.getMessage());
        return new CustomErrorResponse(Calendar.getInstance().getTime(), ex.getMessage());
    }

    @ExceptionHandler({FileStoreServiceException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CustomErrorResponse handle(FileStoreServiceException ex) {
        log.info("File storage service encounter something wrong {}.", ex.getMessage());
        return new CustomErrorResponse(Calendar.getInstance().getTime(), ex.getMessage());
    }

    @ExceptionHandler(InvalidFieldException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handle(InvalidFieldException ife) {
        log.info("Invalid field value {}", ife.getMessage());
        return new CustomErrorResponse(Calendar.getInstance().getTime(), ife.getMessage());
    }

}
