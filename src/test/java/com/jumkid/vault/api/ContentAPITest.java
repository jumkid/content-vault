package com.jumkid.vault.api;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.EnableTestContainers;
import com.jumkid.vault.TestObjectsBuilder;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import com.jumkid.vault.util.FileUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.io.FileInputStream;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.jumkid.vault.TestObjectsBuilder.DUMMY_ID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:10092", "port=10092" })
@EnableTestContainers
@TestPropertySource("/application.share.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContentAPITest {

    @LocalServerPort
    private int port;

    @Value("${com.jumkid.jwt.test.user-token}")
    private String testUserToken;
    @Value("${com.jumkid.jwt.test.admin-token}")
    private String testAdminToken;
    @Value("file:src/test/resources/icon_file.png")
    private Resource fileResource;

    @MockBean
    private LocalFileStorage localFileStorage;
    @MockBean
    private MetadataStorage metadataStorage;

    private MediaFileMetadata mediaFileMetadata;

    @BeforeAll
    void setup() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            mediaFileMetadata = TestObjectsBuilder.buildMetadata(null);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    @Order(1)
    void whenGivenTitleAndContent_shouldSaveHtmlContent() throws Exception{
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testUserToken)
                    .queryParam("title", mediaFileMetadata.getTitle())
                    .queryParam("accessScope", AccessScope.PUBLIC.value())
                    .body(mediaFileMetadata.getContent())
                .when()
                    .post("/content")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("uuid", equalTo(DUMMY_ID),
                            "title", equalTo("test.title"));
    }

    @Test
    void whenGivenId_shouldGetTextContentWithTitle() throws Exception {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .contentType(ContentType.TEXT)
                .when()
                    .get("/content/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(containsString("test.title"));
    }

    @Test
    void whenGivenIdAndIgnoreTitle_shouldGetTextContentWithoutTitle() throws Exception {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .contentType(ContentType.TEXT)
                    .queryParam("ignoreTitle", "true")
                .when()
                    .get("/content/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body(not(containsString("test.title")))
                    .body(containsString("test.content"));
    }

    @Test
    void shouldGet404WithInvalidId_whenGivenInvalidId() throws Exception {
        when(metadataStorage.getMetadata("InvalidId")).thenReturn(Optional.empty());

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.TEXT)
                .when()
                    .get("/content/InvalidId")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldGet400WithoutId() throws Exception {
        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testAdminToken)
                    .contentType(ContentType.TEXT)
                .when()
                    .get("/content")
                .then()
                    .log()
                    .all()
                    .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldGetThumbnail()throws Exception {
        MediaFileMetadata mediaFileMetadata = TestObjectsBuilder.buildMetadata(null);
        Optional<byte[]> fileByte = Optional.empty();
        try (FileInputStream fin = new FileInputStream(fileResource.getFile())) {
            fileByte = FileUtils.fileChannelToBytes(fin.getChannel());
        } catch(Exception e) {
            fail();
        }
        when(localFileStorage.getThumbnail(eq(mediaFileMetadata), any(ThumbnailNamespace.class))).thenReturn(fileByte);
        when(localFileStorage.getFileBinary(mediaFileMetadata))
                .thenReturn(Optional.of(mediaFileMetadata.getContent().getBytes()));
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));

        RestAssured
                .given()
                    .baseUri("http://localhost").port(port)
                    .headers("Authorization", "Bearer " + testUserToken)
                    .contentType(ContentType.BINARY)
                .when()
                    .get("/content/thumbnail/" + DUMMY_ID)
                .then()
                    .statusCode(HttpStatus.OK.value());
    }
}
