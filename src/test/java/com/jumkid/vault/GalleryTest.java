package com.jumkid.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.jumkid.vault.TestsSetup.DUMMY_ID;

@SpringBootTest(properties = { "jwt.token.enable = false" })
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GalleryTest {

    @Value("file:src/test/resources/upload-test.html")
    private Resource fileResource;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata galleryMetadata;

    @BeforeEach
    void setup() {
        try {
            galleryMetadata = TestsSetup.buildGalleryMetadata(null);

            when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(galleryMetadata));
            when(metadataStorage.updateMetadata(DUMMY_ID, galleryMetadata)).thenReturn(galleryMetadata);

        } catch (Exception e) {
            fail();
        }
    }

    @Test
    @WithMockUser(username="test", password="test", authorities="USER_ROLE")
    void shouldUpdateGallery() throws Exception {
        mockMvc.perform(put("/gallery/" + DUMMY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsBytes(MediaFile.builder()
                        .uuid(DUMMY_ID).title("test update gallery").content("test update gallery content")
                        .build())))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(username="test", password="test", authorities="USER_ROLE")
    void shouldUpdateGalleryWithChild() throws Exception {
        mockMvc.perform(post("/gallery/" + DUMMY_ID)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("mediaFileIds", "1", "2"))
                .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(username="admin", password="admin", authorities="ADMIN_ROLE")
    void shouldCloneGallery() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(galleryMetadata);

        mockMvc.perform(post("/gallery/" + DUMMY_ID + "/clone")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

}
