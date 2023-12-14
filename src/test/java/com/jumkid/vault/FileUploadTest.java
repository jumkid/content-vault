package com.jumkid.vault;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.repository.MetadataStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "jwt.token.enable = false" })
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileUploadTest {

    @Value("file:src/test/resources/upload-test.html")
    private Resource resource;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataStorage metadataStorage;

    @MockBean
    private LocalFileStorage localFileStorage;

    private MediaFileMetadata mediaFileMetadata;

    @BeforeAll
    void setup() {
        try {
            mediaFileMetadata = TestsSetup.buildMetadata(null);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    @WithMockUser(username="test", password="test", authorities="USER_ROLE")
    void whenGivenFile_shouldUploadFile() throws Exception {

        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(any(), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(any(), any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        byte[] uploadFile = Files.readAllBytes(Paths.get(resource.getFile().getPath()));

        mockMvc.perform(multipart("/file/upload")
                .file("file", uploadFile)
                .param("accessScope", AccessScope.PUBLIC.value()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.filename").value(mediaFileMetadata.getFilename()));
    }

    @Test
    @WithMockUser(username="test", password="test", authorities="USER_ROLE")
    void whenGivenFile_shouldUploadMultipleFile() throws Exception {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(any(), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(any(), any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);

        byte[] uploadFile = Files.readAllBytes(Paths.get(resource.getFile().getPath()));

        mockMvc.perform(multipart("/file/multipleUpload")
                .file("files", uploadFile)
                .param("accessScope", AccessScope.PUBLIC.value()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$[0].filename").value(mediaFileMetadata.getFilename()));
    }

}
