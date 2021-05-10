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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = { "jwt.token.enable = false" })
@AutoConfigureMockMvc
public class FileUploadAPITest extends APITestsSetup {

    @Value("file:src/test/resources/upload-test.html")
    private Resource resource;

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
        } catch (Exception e) {
            Assert.fail();
        }

    }

    @Test
    @WithMockUser(username="test",
            password="test",
            authorities="user")
    public void whenGivenFile_shouldUploadFile() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(any(), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        byte[] uploadFile = Files.readAllBytes(Paths.get(resource.getFile().getPath()));

        mockMvc.perform(multipart("/file/upload").file("file", uploadFile))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.filename").value(mediaFileMetadata.getFilename()));
    }

    @Test
    @WithMockUser(username="test",
            password="test",
            authorities="user")
    public void whenGivenFile_shouldUploadMultipleFile() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(any(), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        byte[] uploadFile = Files.readAllBytes(Paths.get(resource.getFile().getPath()));

        mockMvc.perform(multipart("/file/multipleUpload").file("files", uploadFile))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$[0].filename").value(mediaFileMetadata.getFilename()));
    }

}
