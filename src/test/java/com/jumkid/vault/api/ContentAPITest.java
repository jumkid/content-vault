package com.jumkid.vault.api;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.TestContainerBase;
import com.jumkid.vault.TestObjectsBuilder;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.util.FileUtils;
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

import java.io.FileInputStream;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContentAPITest extends TestContainerBase implements TestObjectsBuilder {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Value("file:src/test/resources/icon_file.png")
    private Resource fileResource;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata mediaFileMetadata;

    @BeforeAll
    void setup() {
        esContainer.start();
        try {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

            this.mediaFileMetadata = buildMetadata(null);

        } catch (Exception e) {
            fail();
        }

    }

    @Test
    @Order(1)
    @WithMockUser(authorities = "USER_ROLE")
    void whenGivenTitleAndContent_shouldSaveHtmlContent() throws Exception{
        //when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        RestAssuredMockMvc
                .given()
                    .header("Accept", "application/json")
                    .param("title", mediaFileMetadata.getTitle())
                    .param("accessScope", AccessScope.PUBLIC.value())
                    .param("content", mediaFileMetadata.getContent())
                .when()
                    .post("/content")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("uuid", not(equalTo(DUMMY_ID)),
                            "title", equalTo("test.title"));
    }

    @Test
    @Disabled
    @WithMockUser(authorities = "GUEST_ROLE")
    void whenGivenId_shouldGetTextContentWithTitle() throws Exception {
        //when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));

        RestAssuredMockMvc
                .given()
                    .contentType(MediaType.TEXT_PLAIN)
                .when()
                    .get("/content/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(containsString("test.title"));
    }

    @Test
    @Disabled
    @WithMockUser(username="guest", password="guest", authorities="GUEST_ROLE")
    void whenGivenIdAndIgnoreTitle_shouldGetTextContentWithoutTitle() throws Exception {
        //when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));

        RestAssuredMockMvc
                .given()
                    .contentType(MediaType.TEXT_PLAIN)
                    .queryParam("ignoreTitle", "true")
                .when()
                    .get("/content/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(not(containsString("test.title")))
                    .body(containsString("test.content"));
    }

    @Test
    @WithMockUser(authorities = "USER_ROLE")
    void shouldGet404WithInvalidId_whenGivenInvalidId() throws Exception {
        //when(metadataStorage.getMetadata("InvalidId")).thenReturn(Optional.empty());

        RestAssuredMockMvc
                .given()
                    .contentType(MediaType.TEXT_PLAIN)
                .when()
                    .get("/content/InvalidId")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @WithMockUser(username="guest", password="guest", authorities="GUEST_ROLE")
    void shouldGet400WithoutId() throws Exception {
        RestAssuredMockMvc
                .given()
                    .contentType(MediaType.TEXT_PLAIN)
                .when()
                    .get("/content")
                .then()
                    .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    @Test
    @Disabled
    @WithMockUser(authorities = "USER_ROLE")
    void shouldGetThumbnail()throws Exception {
        MediaFileMetadata mediaFileMetadata = buildMetadata(null);
        Optional<byte[]> fileByte = Optional.empty();
        try (FileInputStream fin = new FileInputStream(fileResource.getFile())) {
            fileByte = FileUtils.fileChannelToBytes(fin.getChannel());
        } catch(Exception e) {
            fail();
        }
        when(localFileStorage.getThumbnail(eq(mediaFileMetadata), any(ThumbnailNamespace.class))).thenReturn(fileByte);
        when(localFileStorage.getFileBinary(mediaFileMetadata))
                .thenReturn(Optional.of(mediaFileMetadata.getContent().getBytes()));
        //when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));

        RestAssuredMockMvc
                .given()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .when()
                    .get("/content/thumbnail/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.OK.value());
    }

}
