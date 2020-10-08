package com.jumkid.vault;

import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.ESContentStorage;
import com.jumkid.vault.repository.LocalFileStorage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class WebAPITests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ESContentStorage esContentStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private static String DUMMY_ID = "dummy-id";
    private static String INVALID_ID = "invalid-id";

    @Before
    public void setup() {
        MediaFileMetadata mediaFileMetadata = buildMetadata();

        when(esContentStorage.getMetadata(DUMMY_ID)).thenReturn(mediaFileMetadata);
        when(localFileStorage.getFileBinary(mediaFileMetadata))
                .thenReturn(Optional.of(mediaFileMetadata.getContent().getBytes()));
    }

    @Test
    public void shouldGetPlainContentWithTitle() throws Exception{
        MvcResult result = mockMvc.perform(get("/content/plain/"+DUMMY_ID)
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assert.assertTrue(content.contains("test.title"));
        Assert.assertTrue(content.contains("test.content"));
    }

    @Test
    public void shouldGetPlainContentWithoutTitle() throws Exception{
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
    public void shouldGet404WithInvalidId() throws Exception{
        mockMvc.perform(get("/content/plain/"+INVALID_ID)
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldGet400WithoutId() throws Exception{
        mockMvc.perform(get("/content/plain")
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldGetHtml() throws Exception{
        MvcResult result = mockMvc.perform(get("/content/html/"+DUMMY_ID)
                .contentType(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assert.assertFalse(content.contains("test.title"));
        Assert.assertEquals("<p>test.content</p>", content);
    }

    private MediaFileMetadata buildMetadata() {
        return MediaFileMetadata.builder()
                .id("dummy-id").title("test.title").size(0).filename("test.file")
                .content("<p>test.content</p>")
                .build();
    }

}
