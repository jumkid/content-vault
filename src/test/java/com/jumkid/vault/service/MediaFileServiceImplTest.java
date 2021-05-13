package com.jumkid.vault.service;

import com.jumkid.vault.TestsSetup;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.MetadataStorage;
import com.jumkid.vault.repository.HadoopFileStorage;
import com.jumkid.vault.repository.LocalFileStorage;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
public class MediaFileServiceImplTest extends TestsSetup {

    @Mock
    private MetadataStorage metadataStorage;
    @Mock
    private HadoopFileStorage hadoopFileStorage;
    @Mock
    private LocalFileStorage localFileStorage;
    @Mock
    private MediaFileSecurityService securityService;

    private MediaFileServiceImpl mediaFileService;

    @Before
    public void setup(){
        when(securityService.getCurrentUserName()).thenReturn("admin");

        mediaFileService = new MediaFileServiceImpl(metadataStorage, hadoopFileStorage, localFileStorage, securityService);
        mediaFileService.setStorageMode("local");
    }

    @Test
    public void shouldAddMediaFileWithoutBytes() {
        final MediaFile mediaFile = buildMediaFile(null);
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(buildMetadata(null));
        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, MediaFileModule.TEXT);

        Assertions.assertThat(savedMediaFile).isEqualTo(mediaFile);
    }

    @Test
    public void shouldAddMediaFileWithBytes() {
        final MediaFile mediaFile = buildMediaFile(null);
        final MediaFileMetadata mediaFileMetadata = buildMetadata(null);
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(eq(mediaFile.getFile()), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, MediaFileModule.TEXT);

        Assertions.assertThat(savedMediaFile).isEqualTo(mediaFile);
    }

    @Test
    public void shouldAddMediaGalleryWithBytes() {
        final MediaFile mediaFile = this.buildMediaGallery(null);
        final MediaFileMetadata mediaFileMetadata = buildGalleryMetadata(null);
        MediaFileMetadata child1 = mediaFileMetadata.getChildren().get(0);
        MediaFileMetadata child2 = mediaFileMetadata.getChildren().get(1);
        when(metadataStorage.saveMetadata(eq(mediaFileMetadata))).thenReturn(mediaFileMetadata);
        when(metadataStorage.saveMetadata(eq(child1))).thenReturn(child1);
        when(metadataStorage.saveMetadata(eq(child2))).thenReturn(child2);

        MediaFile savedMediaFile = mediaFileService.addMediaGallery(mediaFile);

        Assertions.assertThat(savedMediaFile).isEqualTo(mediaFile);
    }

}
