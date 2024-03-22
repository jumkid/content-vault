package com.jumkid.vault;

import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startables;

import java.util.HashMap;
import java.util.Map;

@DisplayName("No tests should be placed here")
public class TestContainerConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

//    @Value("${spring.testcontainers.elasticsearch-image}")
//    private String elasticsearchImage;

    static final ElasticsearchContainer esContainer =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.10.4")
                    .withReuse(true);

    static {
        Startables.deepStart(esContainer).join();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("elasticsearch.host", esContainer.getHost());
        stringMap.put("elasticsearch.port", esContainer.getFirstMappedPort().toString());
        TestPropertyValues.of(stringMap).applyTo(applicationContext.getEnvironment());
    }
}
