package com.jumkid.vault;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.MetadataStorage;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.util.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.FileInputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = { "jwt.token.enable = false" })
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContentTest {

    @Value("file:src/test/resources/icon_file.png")
    private Resource fileResource;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata mediaFileMetadata;

    @BeforeEach
    void setup() {
        try {
            mediaFileMetadata = TestsSetup.buildMetadata(null);

            when(metadataStorage.getMetadata(TestsSetup.DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));
            when(localFileStorage.getFileBinary(mediaFileMetadata))
                    .thenReturn(Optional.of(mediaFileMetadata.getContent().getBytes()));
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    @WithMockUser(username="test", password="test", authorities="USER_ROLE")
    void shouldSaveHtmlContent_whenGivenTitleAndContent() throws Exception{
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        mockMvc.perform(post("/content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("title", mediaFileMetadata.getTitle())
                    .param("accessScope", AccessScope.PUBLIC.value())
                    .param("content", mediaFileMetadata.getContent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(mediaFileMetadata.getTitle()))
                .andExpect(jsonPath("$.content").value(mediaFileMetadata.getContent()));
    }

    @Test
    @WithMockUser(username="guest", password="guest", authorities="GUEST_ROLE")
    void shouldGetTextContentWithTitle_whenGivenId() throws Exception {
        MvcResult result = mockMvc.perform(get("/content/" + TestsSetup.DUMMY_ID)
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("test.title"));
        assertTrue(content.contains("test.content"));
    }

    @Test
    @WithMockUser(username="guest", password="guest", authorities="GUEST_ROLE")
    void shouldGetTextContentWithoutTitle_whenGivenIdAndIgnoreTitle() throws Exception {
        MvcResult result = mockMvc.perform(get("/content/" + TestsSetup.DUMMY_ID)
                .contentType(MediaType.TEXT_PLAIN)
                .queryParam("ignoreTitle", "true"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertFalse(content.contains("test.title"));
        assertTrue(content.contains("test.content"));
    }

    @Test
    @WithMockUser(username="guest", password="guest", authorities="GUEST_ROLE")
    void shouldGet404WithInvalidId_whenGivenInvalidId() throws Exception {
        mockMvc.perform(get("/content/InvalidId")
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username="guest", password="guest", authorities="GUEST_ROLE")
    void shouldGet400WithoutId() throws Exception {
        mockMvc.perform(get("/content")
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username="guest", password="guest", authorities="GUEST_ROLE")
    void shouldGetThumbnail()throws Exception {
        MediaFileMetadata mediaFileMetadata = TestsSetup.buildMetadata(null);
        Optional<byte[]> fileByte = Optional.empty();
        try (FileInputStream fin = new FileInputStream(fileResource.getFile())) {
            fileByte = FileUtils.fileChannelToBytes(fin.getChannel());
        } catch(Exception e) {
            fail();
        }
        when(localFileStorage.getThumbnail(eq(mediaFileMetadata), any(ThumbnailNamespace.class))).thenReturn(fileByte);

        MvcResult result = mockMvc.perform(get("/content/thumbnail/" + TestsSetup.DUMMY_ID)
                .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] responseFile = result.getResponse().getContentAsByteArray();
        assertTrue(fileByte.isPresent());
        assertEquals(responseFile.length, fileByte.get().length);
    }

}
