package com.jumkid.vault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestContentVaultApplication {

    @Value("${spring.testcontainers.elasticsearch-image}")
    private String elasticsearchImage;

    @Bean
    @RestartScope
    //@ServiceConnection
    ElasticsearchContainer elasticsearchContainer() {
        return new ElasticsearchContainer(DockerImageName.parse(elasticsearchImage)).withReuse(true);
    }

    public static void main(String... args) {
        SpringApplication.from(ContentVaultApplication::main).with(TestContentVaultApplication.class).run(args);
    }

}
