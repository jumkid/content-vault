package com.jumkid.vault.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.EnableTestContainers;
import com.jumkid.vault.TestObjectsBuilder;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static com.jumkid.vault.TestObjectsBuilder.DUMMY_ID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableTestContainers
@TestPropertySource("/application.share.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetadataAPITest {

    @LocalServerPort
    private int port;

    @Value("${com.jumkid.jwt.test.user-token}")
    private String testUserToken;
    @Value("${com.jumkid.jwt.test.admin-token}")
    private String testAdminToken;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata mediaFileMetadata;

    @BeforeAll
    void setup() {
        try {
            RestAssured.defaultParser = Parser.JSON;

            mediaFileMetadata = TestObjectsBuilder.buildMetadata(null);
            when(localFileStorage.getFileBinary(mediaFileMetadata))
                    .thenReturn(Optional.of(mediaFileMetadata.getContent().getBytes()));
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    void whenGivenId_shouldGetMetadata() {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.JSON)
                .when()
                    .get("/metadata/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("uuid", equalTo(DUMMY_ID),
                        "title", equalTo("test.title"));
    }

    @Test
    void whenSearch_shouldGetListOfMetadata() {
        when(metadataStorage.searchMetadata(anyString(), anyInt(), anyList(), anyString()))
                .thenReturn(TestObjectsBuilder.buildListOfMetadata());

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.JSON)
                .when()
                    .get("/metadata?q=test&size=1")
                .then()
                    .log()
                    .all()
                    .statusCode(HttpStatus.OK.value())
                    .body("[0].uuid", equalTo(DUMMY_ID));
    }

    @Test
    void whenGivenMetadata_shouldSaveContentWithPros() throws Exception {
        MediaFile mediaFile = TestObjectsBuilder.buildMediaFile(null);
        when(metadataStorage.saveMetadata(ArgumentMatchers.any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        RestAssured
            .given()
                .baseUri("http://localhost").port(port)
                .headers("Authorization", "Bearer " + testUserToken)
                .contentType(ContentType.JSON)
                .queryParam("mediaFileModule", MediaFileModule.FILE.value())
                .body(new ObjectMapper().writeValueAsBytes(mediaFile))
            .when()
                .post("/metadata")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("title", equalTo(mediaFileMetadata.getTitle()));
    }

    @Test
    @Disabled
    @WithMockUser(authorities = "ADMIN_ROLE")
    void whenGivenMetadata_shouldUpdateMetadata() throws Exception {
        when(metadataStorage.updateMetadata(DUMMY_ID, mediaFileMetadata)).thenReturn(mediaFileMetadata);

        RestAssured
            .given()
                .baseUri("http://localhost").port(port)
                .headers("Authorization", "Bearer " + testUserToken)
                .contentType(ContentType.JSON)
                .body(new ObjectMapper().writeValueAsBytes(mediaFileMetadata))
            .when()
                .put("/metadata/" + DUMMY_ID)
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(DUMMY_ID),
                        "title", "test.title");
    }

    @Test
    void whenGivenId_shouldDeleteMetadata() {
        RestAssured
            .given()
                .baseUri("http://localhost").port(port)
                .headers("Authorization", "Bearer " + testAdminToken)
                .contentType(ContentType.JSON)
            .when()
                .delete("/metadata/" + DUMMY_ID)
            .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

}
