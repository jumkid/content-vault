package com.jumkid.vault;

import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.MetadataStorage;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.util.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = { "jwt.token.enable = false" })
@AutoConfigureMockMvc
public class ContentTest extends TestsSetup {

    @Value("file:src/test/resources/icon_file.png")
    private Resource fileResource;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata mediaFileMetadata;

    @Before
    public void setup() {
        try {
            mediaFileMetadata = buildMetadata(null);

            when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(mediaFileMetadata);
            when(localFileStorage.getFileBinary(mediaFileMetadata))
                    .thenReturn(Optional.of(mediaFileMetadata.getContent().getBytes()));
        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    @WithMockUser(username="test", password="test", authorities="user")
    public void shouldSaveHtmlContent_whenGivenTitleAndContent() throws Exception{
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        mockMvc.perform(post("/content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("title", mediaFileMetadata.getTitle())
                    .param("content", mediaFileMetadata.getContent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(mediaFileMetadata.getTitle()))
                .andExpect(jsonPath("$.content").value(mediaFileMetadata.getContent()));
    }

    @Test
    public void shouldGetTextContentWithTitle_whenGivenId() throws Exception {
        MvcResult result = mockMvc.perform(get("/content/"+DUMMY_ID)
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assert.assertTrue(content.contains("test.title"));
        Assert.assertTrue(content.contains("test.content"));
    }

    @Test
    public void shouldGetTextContentWithoutTitle_whenGivenIdAndIgnoreTitle() throws Exception {
        MvcResult result = mockMvc.perform(get("/content/"+DUMMY_ID)
                .contentType(MediaType.TEXT_PLAIN)
                .queryParam("ignoreTitle", "true"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assert.assertFalse(content.contains("test.title"));
        Assert.assertTrue(content.contains("test.content"));
    }

    @Test
    public void shouldGet404WithInvalidId_whenGivenInvalidId() throws Exception {
        mockMvc.perform(get("/content/InvalidId")
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldGet400WithoutId() throws Exception {
        mockMvc.perform(get("/content")
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldGetThumbnail()throws Exception {
        MediaFileMetadata mediaFileMetadata = buildMetadata(null);
        Optional<byte[]> fileByte = Optional.empty();
        try (FileInputStream fin = new FileInputStream(fileResource.getFile())) {
            fileByte = FileUtils.fileChannelToBytes(fin.getChannel());
        } catch(Exception e) {
            Assert.fail();
        }
        when(localFileStorage.getThumbnail(eq(mediaFileMetadata), any(ThumbnailNamespace.class))).thenReturn(fileByte);

        MvcResult result = mockMvc.perform(get("/content/thumbnail/" + DUMMY_ID)
                .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        byte[] responseFile = result.getResponse().getContentAsByteArray();
        Assert.assertTrue(fileByte.isPresent());
        Assert.assertEquals(responseFile.length, fileByte.get().length);
    }

}
