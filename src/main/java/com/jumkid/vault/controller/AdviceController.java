package com.jumkid.vault.controller;

import com.jumkid.share.controller.response.CustomErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.FileNotFoundException;
import java.util.Calendar;

import org.springframework.http.HttpStatus;

@RestControllerAdvice
@Slf4j
public class AdviceController {

    @ExceptionHandler({FileNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CustomErrorResponse handle(Exception ex) {
        log.info("File could not be found.", ex);
        return new CustomErrorResponse(Calendar.getInstance().getTime(), ex.getMessage());
    }

}
