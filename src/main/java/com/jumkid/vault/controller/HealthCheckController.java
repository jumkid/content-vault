package com.jumkid.vault.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus(HttpStatus.OK)
    public String checkHealthStatus() {
        return String.format("{'status': 'GREEN', 'time':'%s'}", new Date().getTime());
    }

}
