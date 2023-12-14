package com.jumkid.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import com.jumkid.vault.service.MediaFileSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "jwt.token.enable = false" })
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetadataTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    @Autowired
    private MediaFileSecurityService securityService;

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
    @WithMockUser(username="demo1", password="demo", authorities="USER_ROLE")
    void shouldGetMetadata_whenGivenId() throws Exception {
        mockMvc.perform(get("/metadata/" + TestsSetup.DUMMY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(TestsSetup.DUMMY_ID))
                .andExpect(jsonPath("$.title").value("test.title"));
    }

    @Test
    @WithMockUser(username="demo1", password="demo", authorities="USER_ROLE")
    void shouldGetListOfMetadata_whenSearch() throws Exception {
        when(metadataStorage.searchMetadata(anyString(), anyInt(), anyList(), anyString())).thenReturn(TestsSetup.buildListOfMetadata());

        mockMvc.perform(get("/metadata?q=test&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists())
                .andExpect(jsonPath("$[0].uuid").value(TestsSetup.DUMMY_ID))
                .andExpect(jsonPath("$[1]").exists());
    }

    @Test
    @WithMockUser(username="admin", password="admin", authorities="ADMIN_ROLE")
    void shouldSaveContentWithPros_whenGivenMetadata() throws Exception {
        MediaFile mediaFile = TestsSetup.buildMediaFile(null);
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        mockMvc.perform(post("/metadata")
                .contentType(MediaType.APPLICATION_JSON)
                .param("mediaFileModule", MediaFileModule.FILE.value())
                .content(new ObjectMapper().writeValueAsBytes(mediaFile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(mediaFileMetadata.getTitle()));
    }

    @Test
    @WithMockUser(username="demo1", password="demo", authorities="USER_ROLE")
    void shouldUpdateMetadata_whenGivenMetadata() throws Exception {
        when(metadataStorage.updateMetadata(TestsSetup.DUMMY_ID, mediaFileMetadata)).thenReturn(mediaFileMetadata);

        mockMvc.perform(put("/metadata/" + TestsSetup.DUMMY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsBytes(mediaFileMetadata)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(TestsSetup.DUMMY_ID))
                .andExpect(jsonPath("$.title").value("test.title"));
    }

    @Test
    @WithMockUser(username="demo1", password="demo", authorities="USER_ROLE")
    void shouldDeleteMetadata_whenGivenId() throws Exception {
        mockMvc.perform(delete("/metadata/" + TestsSetup.DUMMY_ID))
                .andExpect(status().isNoContent());
    }

}
