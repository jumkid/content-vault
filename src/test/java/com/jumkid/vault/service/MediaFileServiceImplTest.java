package com.jumkid.vault.service;

import com.jumkid.vault.TestsSetup;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileField;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.GalleryNotFoundException;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
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
    public void shouldGetMediaFile() {
        MediaFile mediaFile = buildMediaFile(null);
        MediaFileMetadata mediaFileMetadata = buildMetadata(null);
        String mediaFileId = mediaFile.getUuid();

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(mediaFileMetadata);

        MediaFile mediaFile1 = mediaFileService.getMediaFile(mediaFileId);

        Assertions.assertThat(mediaFile1).isEqualTo(mediaFile);
    }

    @Test
    public void shouldThrowException_WhenGetMediaFileWithInvalidId() {
        String invalidId = "invalidId";

        when(metadataStorage.getMetadata(invalidId)).thenReturn(null);

        Assertions.assertThatExceptionOfType(FileNotFoundException.class)
                .isThrownBy(() -> {mediaFileService.getMediaFile(invalidId);})
                .withMessageContaining(invalidId);
    }

    @Test
    public void shouldThrowException_WhenGetInactivatedMediaFile() {
        MediaFileMetadata mediaFileMetadata = buildMetadata(null);
        String mediaFileId = mediaFileMetadata.getId();
        mediaFileMetadata.setActivated(false);

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(mediaFileMetadata);

        Assertions.assertThatExceptionOfType(FileNotAvailableException.class)
                .isThrownBy(() -> {mediaFileService.getMediaFile(mediaFileId);});
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

    @Test
    public void shouldUpdateMediaFile() {
        final MediaFile mediaFile = this.buildMediaFile(null);
        final String mediaFileId = mediaFile.getUuid();
        final MediaFileMetadata mediaFileMetadata = this.buildMetadata(null);

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(mediaFileMetadata);
        when(metadataStorage.saveMetadata(eq(mediaFileMetadata))).thenReturn(mediaFileMetadata);

        MediaFile savedMediaFile = mediaFileService.updateMediaGallery(mediaFileId, mediaFile);

        Assertions.assertThat(savedMediaFile).isEqualTo(mediaFile);
    }

    @Test
    public void shouldThrowException_WhenUpdateMediaFileWithInvalidId() {
        String invalidId = "invalid_id";
        final MediaFile mediaFile = this.buildMediaFile(null);

        when(metadataStorage.getMetadata(invalidId)).thenReturn(null);

        Assertions.assertThatExceptionOfType(GalleryNotFoundException.class)
                .isThrownBy(() -> {mediaFileService.updateMediaGallery(invalidId, mediaFile);})
                .withMessageContaining(invalidId);
    }

    @Test
    public void shouldGetNull_WhenUpdateNullMediaFile() {
        final MediaFile mediaFile = this.buildMediaFile(null);
        final String mediaFileId = mediaFile.getUuid();

        MediaFile savedGallery = mediaFileService.updateMediaGallery(mediaFileId, null);

        Assertions.assertThat(savedGallery).isNull();
    }

    @Test
    public void shouldUpdateMediaGallery_ByGivenAMediaFileObject() {
        final MediaFile gallery = this.buildMediaGallery(null);
        final MediaFileMetadata galleryMetadata = buildGalleryMetadata(null);
        final String galleryId = gallery.getUuid();

        when(metadataStorage.getMetadata(galleryId)).thenReturn(galleryMetadata);
        when(metadataStorage.saveMetadata(eq(galleryMetadata))).thenReturn(galleryMetadata);

        MediaFile savedGallery = mediaFileService.updateMediaGallery(galleryId, gallery);

        Assertions.assertThat(savedGallery).isEqualTo(gallery);
    }

    @Test
    public void shouldThrowException_WhenUpdateGalleryWithInvalidId() {
        String invalidId = "invalid_id";
        final MediaFile gallery = this.buildMediaGallery(null);

        when(metadataStorage.getMetadata(invalidId)).thenReturn(null);

        Assertions.assertThatExceptionOfType(GalleryNotFoundException.class)
                .isThrownBy(() -> {mediaFileService.updateMediaGallery(invalidId, gallery);})
                .withMessageContaining(invalidId);
    }

    @Test
    public void shouldGetNull_WhenUpdateNullGallery() {
        final MediaFile gallery = this.buildMediaGallery(null);
        final String galleryId = gallery.getUuid();

        MediaFile savedGallery = mediaFileService.updateMediaGallery(galleryId, null);

        Assertions.assertThat(savedGallery).isNull();
    }

    @Test
    public void shouldGetTrue_WhenUpdateMediaFileField() {
        String mediaFileId = "dummyId";
        when(metadataStorage.updateMetadataField(eq(mediaFileId), any(MediaFileField.class), any())).thenReturn(true);

        Assertions.assertThat(mediaFileService.updateMediaFileField(mediaFileId, MediaFileField.TITLE, "test"))
                    .isTrue();
    }

    @Test
    public void shouldGetFalse_WhenUpdateMediaFileFieldWithNull() {
        Assertions.assertThat(mediaFileService.updateMediaFileField(null, MediaFileField.TITLE, "test"))
                    .isFalse();
        Assertions.assertThat(mediaFileService.updateMediaFileField("dummyId", null, "test"))
                .isFalse();
    }

    @Test
    public void shouldGetTrue_WhenUpdateMediaFileFields() {
        String mediaFileId = "dummyId";
        Map<MediaFileField, Object> fieldValueMap = new HashMap<>();
        fieldValueMap.put(MediaFileField.TITLE, "test");

        when(metadataStorage.updateMultipleMetadataFields(eq(mediaFileId), eq(fieldValueMap))).thenReturn(true);

        Assertions.assertThat(mediaFileService.updateMediaFileFields(mediaFileId, fieldValueMap))
                    .isTrue();
    }

    @Test
    public void shouldGetFalse_WhenUpdateMediaFileFieldsWithNull() {
        String mediaFileId = "dummyId";
        Map<MediaFileField, Object> fieldValueMap = new HashMap<>();
        fieldValueMap.put(MediaFileField.TITLE, "test");

        Assertions.assertThat(mediaFileService.updateMediaFileFields(null, fieldValueMap))
                .isFalse();
        Assertions.assertThat(mediaFileService.updateMediaFileFields(mediaFileId, null))
                .isFalse();
    }

}
