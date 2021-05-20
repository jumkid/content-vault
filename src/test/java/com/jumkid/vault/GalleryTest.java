package com.jumkid.vault;

import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = { "jwt.token.enable = false" })
@AutoConfigureMockMvc
public class GalleryTest extends TestsSetup{

    @Value("file:src/test/resources/upload-test.html")
    private Resource fileResource;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata galleryMetadata;

    @Before
    public void setup() {
        try {
            galleryMetadata = buildGalleryMetadata(null);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    @WithMockUser(username="test", password="test", authorities="user")
    public void shouldAddGallery_withGivenTitleAndContent() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(galleryMetadata);

        mockMvc.perform(post("/gallery")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .param("title", galleryMetadata.getTitle())
                .param("content", galleryMetadata.getContent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(galleryMetadata.getTitle()))
                .andExpect(jsonPath("$.content").value(galleryMetadata.getContent()))
                .andExpect(jsonPath("$.tags[0]").value(galleryMetadata.getTags().get(0)))
                .andExpect(jsonPath("$.children[0].uuid").value("1"));
    }

    @Test
    @WithMockUser(username="test", password="test", authorities="user")
    public void shouldUpdateGallery() throws Exception {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(galleryMetadata);
        when(metadataStorage.updateMultipleMetadataFields(eq(DUMMY_ID), anyMap())).thenReturn(true);

        mockMvc.perform(put("/gallery/" + DUMMY_ID)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .param("title", galleryMetadata.getTitle())
                .param("content", galleryMetadata.getContent()))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(username="test", password="test", authorities="user")
    public void shouldGetError_whenUpdateGalleryWithoutValidId() throws Exception {
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(galleryMetadata);
        when(metadataStorage.updateMultipleMetadataFields(eq(DUMMY_ID), anyMap())).thenReturn(false);

        mockMvc.perform(put("/gallery/" + DUMMY_ID)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(status().isExpectationFailed());
    }

}
