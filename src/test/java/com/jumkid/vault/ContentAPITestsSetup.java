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

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = { "jwt.token.enable = false" })
@AutoConfigureMockMvc
public class ContentAPITestsSetup extends APITestsSetup {

    @Value("file:src/test/resources/upload-test.html")
    private Resource resource;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

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
    public void whenGivenMetadata_shouldSaveContentWithPros() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        mockMvc.perform(post("/content")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsBytes(mediaFileMetadata)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(mediaFileMetadata.getTitle()))
                .andExpect(jsonPath("$.props[0].name").value(mediaFileMetadata.getProps().get(0).getName()));
    }

    @Test
    public void whenGivenTitleAndContent_shouldSaveSimpleContent() throws Exception{
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        mockMvc.perform(post("/content/plain")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("title", mediaFileMetadata.getTitle())
                    .param("content", mediaFileMetadata.getContent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(mediaFileMetadata.getTitle()))
                .andExpect(jsonPath("$.content").value(mediaFileMetadata.getContent()));
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

}
