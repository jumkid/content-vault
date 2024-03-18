package com.jumkid.vault.api;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.TestObjectsBuilder;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@PropertySource("classpath:application.share.properties")
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:10092", "port=10092" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileUploadAPITest {
    @LocalServerPort
    private int port;

    @Value("${com.jumkid.jwt.test.user-token}")
    private String testUserToken;
    @Value("${com.jumkid.jwt.test.user-id}")
    private String testUserId;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Value("file:src/test/resources/upload-test.html")
    private Resource resource;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata mediaFileMetadata;

    @BeforeAll
    void setup() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

            mediaFileMetadata = TestObjectsBuilder.buildMetadata(null);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    void whenGivenFile_shouldUploadFile() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(any(), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(any(), any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.MULTIPART)
                    .multiPart("file", resource.getFile())
                    .multiPart("accessScope", AccessScope.PUBLIC.value())
                .when()
                    .post("/file/upload")
                .then()
                    .log()
                    .all()
                    .statusCode(HttpStatus.ACCEPTED.value())
                    .body("filename", equalTo(mediaFileMetadata.getFilename()));
    }

    @Test
    void whenGivenFile_shouldUploadMultipleFile() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(any(), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(any(), any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.MULTIPART)
                    .multiPart("files", resource.getFile())
                    .multiPart("accessScope", AccessScope.PUBLIC.value())
                .when()
                    .post("/file/multipleUpload")
                .then()
                    .statusCode(HttpStatus.ACCEPTED.value())
                    .body("[0].filename", equalTo(mediaFileMetadata.getFilename()));
    }

}
