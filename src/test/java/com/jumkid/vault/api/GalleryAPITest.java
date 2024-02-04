package com.jumkid.vault.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.TestObjectsBuilder;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GalleryAPITest implements TestObjectsBuilder {
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
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
            galleryMetadata = buildGalleryMetadata(null);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    @WithMockUser(authorities = "USER_ROLE")
    void shouldUpdateGallery() throws Exception {
        //given
        MediaFile gallery = MediaFile.builder()
                .uuid(DUMMY_ID).title("test update gallery").content("test update gallery content")
                .build();
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(galleryMetadata));
        when(metadataStorage.updateMetadata(DUMMY_ID, galleryMetadata)).thenReturn(galleryMetadata);

        RestAssuredMockMvc
                .given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ObjectMapper().writeValueAsBytes(gallery))
                .when()
                    .put("/gallery/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.ACCEPTED.value());
    }

    @Test
    @WithMockUser(authorities = "USER_ROLE")
    void shouldUpdateGalleryWithChild() throws Exception {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(galleryMetadata));
        when(metadataStorage.updateMetadata(DUMMY_ID, galleryMetadata)).thenReturn(galleryMetadata);

        RestAssuredMockMvc
                .given()
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .param("mediaFileIds", "1", "2")
                .when()
                    .post("/gallery/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.ACCEPTED.value());
    }

    @Test
    @WithMockUser(authorities = "ADMIN_ROLE")
    void shouldCloneGallery() throws Exception {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(galleryMetadata));
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(galleryMetadata);

        RestAssuredMockMvc
                .given()
                    .contentType(MediaType.APPLICATION_JSON)
                .when()
                    .post("/gallery/" + DUMMY_ID + "/clone")
                .then()
                    .statusCode(HttpStatus.OK.value());
    }

}
