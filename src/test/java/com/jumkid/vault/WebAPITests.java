package com.jumkid.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.MetadataStorage;
import com.jumkid.vault.repository.LocalFileStorage;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class WebAPITests {

    @Value("file:src/test/resources/upload-test.html")
    private Resource resource;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private static String DUMMY_ID = "dummy-id";
    private static String INVALID_ID = "invalid-id";
    private MediaFileMetadata mediaFileMetadata;

    @Before
    public void setup() {
        try {
            mediaFileMetadata = buildMetadata();

            when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(mediaFileMetadata);
            when(localFileStorage.getFileBinary(mediaFileMetadata))
                    .thenReturn(Optional.of(mediaFileMetadata.getContent().getBytes()));
        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    public void whenGivenId_shouldGetPlainContentWithTitle() throws Exception {
        MvcResult result = mockMvc.perform(get("/content/plain/"+DUMMY_ID)
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assert.assertTrue(content.contains("test.title"));
        Assert.assertTrue(content.contains("test.content"));
    }

    @Test
    public void whenGivenIdAndIgnoreTitle_shouldGetPlainContentWithoutTitle() throws Exception {
        MvcResult result = mockMvc.perform(get("/content/plain/"+DUMMY_ID)
                .contentType(MediaType.TEXT_PLAIN)
                .queryParam("ignoreTitle", "true"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assert.assertFalse(content.contains("test.title"));
        Assert.assertTrue(content.contains("test.content"));
    }

    @Test
    public void whenGivenInvalidId_shouldGet404WithInvalidId() throws Exception {
        mockMvc.perform(get("/content/plain/"+INVALID_ID)
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldGet400WithoutId() throws Exception {
        mockMvc.perform(get("/content/plain")
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void whenGivenId_shouldGetHtml() throws Exception {
        MvcResult result = mockMvc.perform(get("/content/html/"+DUMMY_ID)
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assert.assertFalse(content.contains("test.title"));
        Assert.assertEquals("<p>test.content</p>", content);
    }

    @Test
    public void whenGivenFile_shouldUploadFile() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(any(), any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(metadataStorage.updateMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        byte[] uploadFile = Files.readAllBytes(Paths.get(resource.getFile().getPath()));

        mockMvc.perform(multipart("/file/upload").file("file", uploadFile))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.filename").value(mediaFileMetadata.getFilename()));
    }

    @Test
    public void shouldGetListOfMetadata() throws Exception {
        when(metadataStorage.getAll()).thenReturn(buildListOfMetadata());

        mockMvc.perform(get("/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists())
                .andExpect(jsonPath("$[0].uuid").value(DUMMY_ID))
                .andExpect(jsonPath("$[1]").exists());
    }

    @Test
    public void whenGivenID_shouldGetMetadata() throws Exception {
        mockMvc.perform(get("/metadata/"+DUMMY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(DUMMY_ID))
                .andExpect(jsonPath("$.title").value("test.title"));
    }

    @Test
    public void whenGivenMetadata_shouldUpdateMetadata() throws Exception {
        mockMvc.perform(put("/metadata/"+DUMMY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsBytes(mediaFileMetadata)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(DUMMY_ID))
                .andExpect(jsonPath("$.title").value("test.title"));
    }

    @Test
    public void whenGivenId_shouldDeleteMetadata() throws Exception {
        mockMvc.perform(delete("/metadata/"+DUMMY_ID))
                .andExpect(status().isNoContent());
    }

    private MediaFileMetadata buildMetadata() throws IOException {
        return MediaFileMetadata.builder()
                .id(DUMMY_ID).title("test.title").filename("upload-test.html")
                .content("<p>test.content</p>").size(Long.valueOf(resource.getFile().length()).intValue())
                .build();
    }

    private List<MediaFileMetadata> buildListOfMetadata() throws IOException {
        final List<MediaFileMetadata> metadataLst = new ArrayList<>();
        MediaFileMetadata mediaFileMetadata1 = buildMetadata();
        MediaFileMetadata mediaFileMetadata2 = MediaFileMetadata.builder()
                .id("dummy-id-1").title("test.title.1")
                .content("<p>test.content.1</p>")
                .build();

        metadataLst.add(mediaFileMetadata1);
        metadataLst.add(mediaFileMetadata2);
        return metadataLst;
    }

}
