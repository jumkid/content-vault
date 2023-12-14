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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:10092", "port=10092" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaFileServiceImplTest {

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

    @BeforeEach
    void setup(){
        mediaFile = TestsSetup.buildMediaFile(null);

        mediaFileService = new MediaFileServiceImpl(metadataStorage, hadoopFileStorage, localFileStorage,
                mediaFileMapper, securityService, metadataEnricher);
        mediaFileService.setStorageMode("local");

        when(securityService.getCurrentUserName()).thenReturn("admin");
        when(metadataStorage.getMetadata(mediaFile.getUuid())).thenReturn(Optional.of(mediaFileMapper.dtoToMetadata(mediaFile)));
    }

    @Test
    void shouldGetMediaFile() {
        MediaFileMetadata mediaFileMetadata = TestsSetup.buildMetadata(null);
        String mediaFileId = mediaFile.getUuid();

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(Optional.of(mediaFileMetadata));

        MediaFile mediaFile1 = mediaFileService.getMediaFile(mediaFileId);

        assertEquals(mediaFile, mediaFile1);
    }

    @Test
    void shouldThrowException_WhenGetMediaFileWithInvalidId() {
        String invalidId = "invalidId";

        when(metadataStorage.getMetadata(invalidId)).thenReturn(Optional.empty());

        assertThrowsExactly(FileNotFoundException.class, () -> mediaFileService.getMediaFile(invalidId));
    }

    @Test
    void shouldThrowException_WhenGetInactivatedMediaFile() {
        MediaFileMetadata mediaFileMetadata = TestsSetup.buildMetadata(null);
        String mediaFileId = mediaFileMetadata.getId();
        mediaFileMetadata.setActivated(false);

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(Optional.of(mediaFileMetadata));

        assertThrowsExactly(FileNotAvailableException.class, () -> mediaFileService.getMediaFile(mediaFileId));
    }

    @Test
    void shouldAddMediaFileWithoutBytes() {
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(TestsSetup.buildMetadata(null));
        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, MediaFileModule.TEXT);

        assertEquals(mediaFile, savedMediaFile);
    }

    @Test
    void shouldAddMediaFileWithBytes() {
        final MediaFileMetadata mediaFileMetadata = TestsSetup.buildMetadata(null);

        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(eq(mediaFile.getFile()), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));

        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, MediaFileModule.TEXT);

        assertEquals(mediaFile, savedMediaFile);
    }

    @Test
    void shouldAddMediaGalleryWithBytes() {
        final MediaFile mediaGallery = TestsSetup.buildMediaGallery(null);
        final MediaFileMetadata mediaFileMetadata = TestsSetup.buildGalleryMetadata(null);
        MediaFileMetadata child1 = mediaFileMetadata.getChildren().get(0);
        MediaFileMetadata child2 = mediaFileMetadata.getChildren().get(1);

        when(metadataStorage.saveMetadata(mediaFileMetadata)).thenReturn(mediaFileMetadata);
        when(metadataStorage.saveMetadata(child1)).thenReturn(child1);
        when(metadataStorage.saveMetadata(child2)).thenReturn(child2);

        MediaFile savedMediaFile = mediaFileService.addMediaGallery(mediaGallery);

        assertEquals(mediaGallery, savedMediaFile);
    }

    @Test
    void shouldUpdateMediaFile() throws IOException {
        final String mediaFileId = mediaFile.getUuid();
        final MediaFileMetadata mediaFileMetadata = TestsSetup.buildMetadata(null);

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(mediaFileId, mediaFileMetadata)).thenReturn(mediaFileMetadata);

        MediaFile savedMediaFile = mediaFileService.updateMediaFile(mediaFileId, mediaFile, null);

        assertEquals(mediaFile, savedMediaFile);
    }

    @Test
    void shouldThrowException_WhenUpdateMediaFileWithInvalidId() {
        String invalidId = "invalid_id";

        when(metadataStorage.getMetadata(invalidId)).thenReturn(Optional.empty());

        assertThrowsExactly(GalleryNotFoundException.class,
                () -> mediaFileService.updateMediaGallery(invalidId, mediaFile));
    }

    @Test
    void shouldGetNull_WhenUpdateNullMediaFile() throws IOException {
        final String mediaFileId = mediaFile.getUuid();

        MediaFile savedGallery = mediaFileService.updateMediaGallery(mediaFileId, null);

        assertNull(savedGallery);
    }

    @Test
    void shouldUpdateMediaGallery_ByGivenAMediaFileObject() throws IOException {
        final MediaFile gallery = TestsSetup.buildMediaGallery(null);
        final MediaFileMetadata galleryMetadata = TestsSetup.buildGalleryMetadata(null);
        final String galleryId = gallery.getUuid();

        when(metadataStorage.getMetadata(galleryId)).thenReturn(Optional.of(galleryMetadata));
        when(metadataStorage.updateMetadata(galleryId, galleryMetadata)).thenReturn(galleryMetadata);

        MediaFile savedGallery = mediaFileService.updateMediaGallery(galleryId, gallery);

        assertEquals(gallery, savedGallery);
    }

    @Test
    void shouldThrowException_WhenUpdateGalleryWithInvalidId() {
        String invalidId = "invalid_id";
        final MediaFile gallery = TestsSetup.buildMediaGallery(null);

        when(metadataStorage.getMetadata(invalidId)).thenReturn(Optional.empty());

        assertThrowsExactly(GalleryNotFoundException.class,
                () -> mediaFileService.updateMediaGallery(invalidId, gallery));
    }

    @Test
    void shouldGetNull_WhenUpdateNullGallery() {
        final MediaFile gallery = TestsSetup.buildMediaGallery(null);
        final String galleryId = gallery.getUuid();

        MediaFile savedGallery = mediaFileService.updateMediaGallery(galleryId, null);

        assertNull(savedGallery);
    }

    @Test
    void shouldGetTrue_WhenUpdateMediaFile() throws IOException {
        when(metadataStorage.updateMetadata(eq(mediaFile.getUuid()), any(MediaFileMetadata.class)))
                .thenReturn(mediaFileMapper.dtoToMetadata(mediaFile));

        MediaFile updateMediaFile = mediaFileService.updateMediaFile(mediaFile.getUuid(), mediaFile, null);
        assertNotNull(updateMediaFile);
    }

    @Test
    void shouldGetNewGallery_WhenCloneMediaGallery() {
        final MediaFile gallery = TestsSetup.buildMediaGallery(null);
        final String galleryId = gallery.getUuid();

        when(metadataStorage.getMetadata(galleryId)).thenReturn(Optional.of(mediaFileMapper.dtoToMetadata(gallery)));
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class)))
                .thenReturn(mediaFileMapper.dtoToMetadata(gallery));

        MediaFile newGallery = mediaFileService.cloneMediaGallery(galleryId, "test");
        assertEquals(gallery.getChildren().size(), newGallery.getChildren().size());
    }

}
