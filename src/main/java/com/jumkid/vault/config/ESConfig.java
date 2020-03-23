package com.jumkid.vault.config;
/*
 * This software is written by Jumkid and subject
 * to a contract between Jumkid and its customer.
 *
 * This software stays property of Jumkid unless differing
 * arrangements between Jumkid and its customer apply.
 *
 *
 * (c)2019 Jumkid Innovation All rights reserved.
 */
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created at 17 Sep, 2018$
 *
 * @author chooliyip
 **/
@Slf4j
@Configuration
public class ESConfig {

    @Value("${elasticsearch.host}")
    private String esHost;

    @Value("${elasticsearch.port1}")
    private int esPort1;

    @Value("${elasticsearch.port2}")
    private int esPort2;

    @Value("${elasticsearch.cluster.name}")
    private String esClusterName;

    @Bean
    public RestHighLevelClient esClient(){

//        Settings esSettings = Settings.builder()
//                .put("cluster.name", esClusterName)
//                .build();

        try {
            return new RestHighLevelClient(RestClient.builder(
                    new HttpHost(InetAddress.getByName(esHost), esPort1, "http"),
                    new HttpHost(InetAddress.getByName(esHost), esPort2, "http")
                    ));
        } catch (UnknownHostException uhe) {
            log.error("Failed to connect elasticsearch host {} ", esHost);
            return null;
        }

    }

}
