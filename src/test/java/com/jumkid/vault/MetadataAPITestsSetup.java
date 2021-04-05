package com.jumkid.vault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.enums.MediaFilePropType;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.model.MediaFileProp;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = { "jwt.token.enable = false" })
@AutoConfigureMockMvc
public class MetadataAPITestsSetup extends APITestsSetup {

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
            mediaFileMetadata = buildMetadata();

            when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(mediaFileMetadata);
            when(localFileStorage.getFileBinary(mediaFileMetadata))
                    .thenReturn(Optional.of(mediaFileMetadata.getContent().getBytes()));
        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    @WithMockUser(username="demo1",
            password="demo",
            authorities="user")
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
    @WithMockUser(username="demo1",
            password="demo",
            authorities="user")
    public void whenGivenID_shouldGetMetadata() throws Exception {
        mockMvc.perform(get("/metadata/"+DUMMY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(DUMMY_ID))
                .andExpect(jsonPath("$.title").value("test.title"));
    }

    @Test
    @WithMockUser(username="demo1",
            password="demo",
            authorities="user")
    public void whenGivenMetadata_shouldUpdateMetadata() throws Exception {
        mockMvc.perform(put("/metadata/"+DUMMY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsBytes(mediaFileMetadata)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(DUMMY_ID))
                .andExpect(jsonPath("$.title").value("test.title"));
    }

    @Test
    @WithMockUser(username="demo1",
            password="demo",
            authorities="user")
    public void whenGivenId_shouldDeleteMetadata() throws Exception {
        mockMvc.perform(delete("/metadata/"+DUMMY_ID))
                .andExpect(status().isNoContent());
    }

    private List<MediaFileMetadata> buildListOfMetadata() throws IOException {
        final List<MediaFileMetadata> metadataLst = new ArrayList<>();
        MediaFileMetadata mediaFileMetadata1 = buildMetadata();
        MediaFileMetadata mediaFileMetadata2 = MediaFileMetadata.builder()
                .id("dummy-id-1").title("test.title.1")
                .content("<p>test.content.1</p>")
                .props(List.of(MediaFileProp.builder()
                        .name("comment").value("I like this").dataType(MediaFilePropType.STRING)
                        .build()))
                .build();

        metadataLst.add(mediaFileMetadata1);
        metadataLst.add(mediaFileMetadata2);
        return metadataLst;
    }
}
