package com.jumkid.vault.api;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.TestObjectsBuilder;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileUploadAPITest implements TestObjectsBuilder {
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
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

            mediaFileMetadata = buildMetadata(null);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    @WithMockUser(authorities = "USER_ROLE")
    void whenGivenFile_shouldUploadFile() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(any(), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(any(), any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        byte[] uploadFile = Files.readAllBytes(Paths.get(resource.getFile().getPath()));

        RestAssuredMockMvc
                .given()
                    .multiPart("file", uploadFile)
                    .queryParam("accessScope", AccessScope.PUBLIC.value())
                .when()
                    .post("/file/upload")
                .then()
                    .statusCode(HttpStatus.ACCEPTED.value())
                    .body("filename", equalTo(mediaFileMetadata.getFilename()));
    }

    @Test
    @WithMockUser(authorities="USER_ROLE")
    void whenGivenFile_shouldUploadMultipleFile() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(any(), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(any(), any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        byte[] uploadFile = Files.readAllBytes(Paths.get(resource.getFile().getPath()));

        RestAssuredMockMvc
                .given()
                    .multiPart("files", uploadFile)
                    .queryParam("accessScope", AccessScope.PUBLIC.value())
                .when()
                    .post("/file/multipleUpload")
                .then()
                    .statusCode(HttpStatus.ACCEPTED.value())
                    .body("[0].filename", equalTo(mediaFileMetadata.getFilename()));
    }

}
