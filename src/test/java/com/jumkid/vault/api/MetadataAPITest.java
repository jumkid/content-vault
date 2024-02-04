package com.jumkid.vault.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.TestContainerBase;
import com.jumkid.vault.TestObjectsBuilder;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetadataAPITest implements TestObjectsBuilder {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata mediaFileMetadata;

    @BeforeAll
    void setup() {
        try {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

            this.mediaFileMetadata = buildMetadata(null);
            when(localFileStorage.getFileBinary(mediaFileMetadata))
                    .thenReturn(Optional.of(mediaFileMetadata.getContent().getBytes()));
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    @WithMockUser(authorities = "USER_ROLE")
    void whenGivenId_shouldGetMetadata() {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));

        RestAssuredMockMvc
                .given()
                    .header("Accept", "application/json")
                .when()
                    .get("/metadata/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("uuid", equalTo(DUMMY_ID),
                        "title", equalTo("test.title"));
    }

    @Test
    @WithMockUser(authorities = "USER_ROLE")
    void whenSearch_shouldGetListOfMetadata() {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

        when(metadataStorage.searchMetadata(anyString(), anyInt(), anyList(), anyString()))
                .thenReturn(buildListOfMetadata());

        RestAssuredMockMvc
                .given()
                    .header("Accept", "application/json")
                .when()
                    .get("/metadata?q=test&size=1")
                .then()
                    .log()
                    .all()
                    .statusCode(HttpStatus.OK.value())
                    .body("[0].uuid", equalTo(DUMMY_ID));
    }

    @Test
    @WithMockUser(authorities = {"USER_ROLE"})
    void whenGivenMetadata_shouldSaveContentWithPros() throws Exception {
        MediaFile mediaFile = buildMediaFile(null);
        when(metadataStorage.saveMetadata(ArgumentMatchers.any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        RestAssuredMockMvc
                .given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
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

        RestAssuredMockMvc
                .given()
                    .header("Accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new ObjectMapper().writeValueAsBytes(mediaFileMetadata))
                .when()
                    .put("/metadata/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("uuid", equalTo(DUMMY_ID),
                            "title", "test.title");
    }

    @Test
    @WithMockUser(authorities = "ADMIN_ROLE")
    void whenGivenId_shouldDeleteMetadata() {
        RestAssuredMockMvc
                .given()
                    .header("Accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                    .delete("/metadata/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
    }

}
