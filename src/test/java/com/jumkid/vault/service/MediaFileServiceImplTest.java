package com.jumkid.vault.service;

import com.jumkid.vault.TestsSetup;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.GalleryNotFoundException;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.MetadataStorage;
import com.jumkid.vault.repository.HadoopFileStorage;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.service.enrich.MetadataEnricher;
import com.jumkid.vault.service.mapper.MediaFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class MediaFileServiceImplTest extends TestsSetup {

    @Mock
    private MetadataStorage metadataStorage;
    @Mock
    private HadoopFileStorage hadoopFileStorage;
    @Mock
    private LocalFileStorage localFileStorage;
    @Autowired
    private MetadataEnricher metadataEnricher;
    @Mock
    private MediaFileSecurityService securityService;

    private MediaFileServiceImpl mediaFileService;

    @Autowired
    private MediaFileMapper mediaFileMapper;

    private MediaFile mediaFile;

    @Before
    public void setup(){
        mediaFile = buildMediaFile(null);

        mediaFileService = new MediaFileServiceImpl(metadataStorage, hadoopFileStorage, localFileStorage,
                mediaFileMapper, securityService, metadataEnricher);
        mediaFileService.setStorageMode("local");

        when(securityService.getCurrentUserName()).thenReturn("admin");
        when(metadataStorage.getMetadata(mediaFile.getUuid())).thenReturn(Optional.of(mediaFileMapper.dtoToMetadata(mediaFile)));
    }

    @Test
    public void shouldGetMediaFile() {
        MediaFileMetadata mediaFileMetadata = buildMetadata(null);
        String mediaFileId = mediaFile.getUuid();

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(Optional.of(mediaFileMetadata));

        MediaFile mediaFile1 = mediaFileService.getMediaFile(mediaFileId);

        Assertions.assertThat(mediaFile1).isEqualTo(mediaFile);
    }

    @Test
    public void shouldThrowException_WhenGetMediaFileWithInvalidId() {
        String invalidId = "invalidId";

        when(metadataStorage.getMetadata(invalidId)).thenReturn(Optional.empty());

        Assertions.assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> {mediaFileService.getMediaFile(invalidId);})
                .withMessageContaining(invalidId);
    }

    @Test
    public void shouldThrowException_WhenGetInactivatedMediaFile() {
        MediaFileMetadata mediaFileMetadata = buildMetadata(null);
        String mediaFileId = mediaFileMetadata.getId();
        mediaFileMetadata.setActivated(false);

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(Optional.of(mediaFileMetadata));

        Assertions.assertThatExceptionOfType(FileNotAvailableException.class)
                .isThrownBy(() -> {mediaFileService.getMediaFile(mediaFileId);});
    }

    @Test
    public void shouldAddMediaFileWithoutBytes() {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(buildMetadata(null));
        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, MediaFileModule.TEXT);

        Assertions.assertThat(savedMediaFile).isEqualTo(mediaFile);
    }

    @Test
    public void shouldAddMediaFileWithBytes() {
        final MediaFileMetadata mediaFileMetadata = buildMetadata(null);

        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(eq(mediaFile.getFile()), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));

        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, MediaFileModule.TEXT);

        Assertions.assertThat(savedMediaFile).isEqualTo(mediaFile);
    }

    @Test
    public void shouldAddMediaGalleryWithBytes() {
        final MediaFile mediaGallery = buildMediaGallery(null);
        final MediaFileMetadata mediaFileMetadata = buildGalleryMetadata(null);
        MediaFileMetadata child1 = mediaFileMetadata.getChildren().get(0);
        MediaFileMetadata child2 = mediaFileMetadata.getChildren().get(1);

        when(metadataStorage.saveMetadata(eq(mediaFileMetadata))).thenReturn(mediaFileMetadata);
        when(metadataStorage.saveMetadata(eq(child1))).thenReturn(child1);
        when(metadataStorage.saveMetadata(eq(child2))).thenReturn(child2);

        MediaFile savedMediaFile = mediaFileService.addMediaGallery(mediaGallery);

        Assertions.assertThat(savedMediaFile).isEqualTo(mediaGallery);
    }

    @Test
    public void shouldUpdateMediaFile() throws IOException {
        final String mediaFileId = mediaFile.getUuid();
        final MediaFileMetadata mediaFileMetadata = this.buildMetadata(null);

        when(metadataStorage.getMetadata(eq(mediaFileId))).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(eq(mediaFileId), eq(mediaFileMetadata))).thenReturn(mediaFileMetadata);

        MediaFile savedMediaFile = mediaFileService.updateMediaFile(mediaFileId, mediaFile, null);

        Assertions.assertThat(savedMediaFile).isEqualTo(mediaFile);
    }

    @Test
    public void shouldThrowException_WhenUpdateMediaFileWithInvalidId() {
        String invalidId = "invalid_id";

        when(metadataStorage.getMetadata(invalidId)).thenReturn(Optional.empty());

        Assertions.assertThatExceptionOfType(GalleryNotFoundException.class)
                .isThrownBy(() -> {mediaFileService.updateMediaGallery(invalidId, mediaFile);})
                .withMessageContaining(invalidId);
    }

    @Test
    public void shouldGetNull_WhenUpdateNullMediaFile() throws IOException {
        final String mediaFileId = mediaFile.getUuid();

        MediaFile savedGallery = mediaFileService.updateMediaGallery(mediaFileId, null);

        Assertions.assertThat(savedGallery).isNull();
    }

    @Test
    public void shouldUpdateMediaGallery_ByGivenAMediaFileObject() throws IOException {
        final MediaFile gallery = this.buildMediaGallery(null);
        final MediaFileMetadata galleryMetadata = buildGalleryMetadata(null);
        final String galleryId = gallery.getUuid();

        when(metadataStorage.getMetadata(eq(galleryId))).thenReturn(Optional.of(galleryMetadata));
        when(metadataStorage.updateMetadata(eq(galleryId), eq(galleryMetadata))).thenReturn(galleryMetadata);

        MediaFile savedGallery = mediaFileService.updateMediaGallery(galleryId, gallery);

        Assertions.assertThat(savedGallery).isEqualTo(gallery);
    }

    @Test
    public void shouldThrowException_WhenUpdateGalleryWithInvalidId() {
        String invalidId = "invalid_id";
        final MediaFile gallery = this.buildMediaGallery(null);

        when(metadataStorage.getMetadata(invalidId)).thenReturn(Optional.empty());

        Assertions.assertThatExceptionOfType(GalleryNotFoundException.class)
                .isThrownBy(() -> {mediaFileService.updateMediaGallery(invalidId, gallery);})
                .withMessageContaining(invalidId);
    }

    @Test
    public void shouldGetNull_WhenUpdateNullGallery() throws IOException {
        final MediaFile gallery = this.buildMediaGallery(null);
        final String galleryId = gallery.getUuid();

        MediaFile savedGallery = mediaFileService.updateMediaGallery(galleryId, null);

        Assertions.assertThat(savedGallery).isNull();
    }

    @Test
    public void shouldGetTrue_WhenUpdateMediaFile() throws IOException {
        when(metadataStorage.updateMetadata(eq(mediaFile.getUuid()), any(MediaFileMetadata.class)))
                .thenReturn(mediaFileMapper.dtoToMetadata(mediaFile));

        MediaFile updateMediaFile = mediaFileService.updateMediaFile(mediaFile.getUuid(), mediaFile, null);
        Assertions.assertThat(updateMediaFile).isNotNull();
    }

    @Test
    public void shouldGetNewGallery_WhenCloneMediaGallery() throws IOException {
        final MediaFile gallery = this.buildMediaGallery(null);
        final String galleryId = gallery.getUuid();

        when(metadataStorage.getMetadata(galleryId)).thenReturn(Optional.of(mediaFileMapper.dtoToMetadata(gallery)));
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class)))
                .thenReturn(mediaFileMapper.dtoToMetadata(gallery));

        MediaFile newGallery = mediaFileService.cloneMediaGallery(galleryId, null);
        Assertions.assertThat(newGallery.getChildren().size()).isSameAs(gallery.getChildren().size());
    }

}
