package com.jumkid.vault;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;

@DisplayName("No tests should be placed here")
public class TestContainerBase {

//    @Value("${spring.testcontainers.elasticsearch-image}")
//    private String elasticsearchImage;

    @Container
    protected final ElasticsearchContainer esContainer =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.10.4")
                    .withReuse(true);
}
