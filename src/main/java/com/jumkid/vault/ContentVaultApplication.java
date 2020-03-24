package com.jumkid.vault;

/*
 * This software is written by Jumkid and subject
 * to a contract between Jumkid and its customer.
 *
 * This software stays property of Jumkid unless differing
 * arrangements between Jumkid and its customer apply.
 *
 *
 * (c)2019 Jumkid All rights reserved.
 *
 */

import com.jumkid.vault.service.mapper.MediaFileMapper;
import lombok.extern.slf4j.Slf4j;

import org.mapstruct.factory.Mappers;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;


/**
 * Main application endpoint with spring boot
 *
 * Created at Sep2018$
 *
 * @author chooliyip
 **/

@Slf4j
@SpringBootApplication
public class ContentVaultApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ContentVaultApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Content Vault Application started with command-line arguments: {} . \n To kill this application, press Ctrl + C.",
                Arrays.toString(args));
    }

    @Bean(name = "restTemplate")
    public RestTemplate restTemplateBean(){
        return new RestTemplate();
    }

    @Bean(name = "mediaFileMapper")
    public MediaFileMapper mediaFileMapperBean() { return Mappers.getMapper( MediaFileMapper.class ); }

}
