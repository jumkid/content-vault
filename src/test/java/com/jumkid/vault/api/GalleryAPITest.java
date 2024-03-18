package com.jumkid.vault.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.TestObjectsBuilder;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static com.jumkid.vault.TestObjectsBuilder.DUMMY_ID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@PropertySource("classpath:application.share.properties")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GalleryAPITest {
    @LocalServerPort
    private int port;
    @Value("${com.jumkid.jwt.test.user-token}")
    private String testUserToken;
    @Value("${com.jumkid.jwt.test.user-id}")
    private String testUserId;
    @Value("${com.jumkid.jwt.test.admin-token}")
    private String testAdminToken;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Value("file:src/test/resources/upload-test.html")
    private Resource fileResource;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata galleryMetadata;

    @BeforeAll
    void setup() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
            galleryMetadata = TestObjectsBuilder.buildGalleryMetadata(null);
            galleryMetadata.setCreatedBy(testUserId);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    void shouldUpdateGallery() throws Exception {
        //given
        MediaFile gallery = MediaFile.builder()
                .uuid(DUMMY_ID).title("test update gallery").content("test update gallery content")
                .build();
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(galleryMetadata));
        when(metadataStorage.updateMetadata(DUMMY_ID, galleryMetadata)).thenReturn(galleryMetadata);

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.JSON)
                    .body(new ObjectMapper().writeValueAsBytes(gallery))
                .when()
                    .put("/gallery/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.ACCEPTED.value());
    }

    @Test
    void shouldUpdateGalleryWithChild() throws Exception {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(galleryMetadata));
        when(metadataStorage.updateMetadata(DUMMY_ID, galleryMetadata)).thenReturn(galleryMetadata);

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.MULTIPART)
                    .multiPart("featuredId", "1")
                .when()
                    .post("/gallery/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.ACCEPTED.value());
    }

    @Test
    @Disabled
    void shouldCloneGallery() throws Exception {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(galleryMetadata));
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(galleryMetadata);

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testAdminToken)
                    .contentType(ContentType.JSON)
                .when()
                    .post("/gallery/" + DUMMY_ID + "/clone")
                .then()
                    .statusCode(HttpStatus.OK.value());
    }

}
