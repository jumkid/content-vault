package com.jumkid.vault.service;

import com.jumkid.share.user.UserProfile;
import com.jumkid.share.user.UserProfileManager;
import com.jumkid.vault.TestObjectsBuilder;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.exception.GalleryNotFoundException;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.MetadataStorage;
import com.jumkid.vault.repository.HadoopFileStorage;
import com.jumkid.vault.repository.LocalFileStorage;
import com.jumkid.vault.service.enrich.MetadataEnricher;
import com.jumkid.vault.service.handler.DTOHandler;
import com.jumkid.vault.service.mapper.MediaFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static com.jumkid.vault.TestObjectsBuilder.DUMMY_ID;

@Slf4j
@SpringBootTest
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
//@EmbeddedKafka(partitions = 1, controlledShutdown = false, brokerProperties = { "listeners=PLAINTEXT://localhost:10092", "port=10092" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaFileServiceImplTest {

    @MockBean
    private MetadataStorage metadataStorage;
    @MockBean
    private HadoopFileStorage hadoopFileStorage;
    @MockBean
    private LocalFileStorage localFileStorage;
    @MockBean
    private UserProfileManager userProfileManager;
    @Autowired
    private MetadataEnricher metadataEnricher;
    @MockBean
    private DTOHandler dtoHandler;
    @MockBean
    private MediaFileSecurityService securityService;

    private MediaFileServiceImpl mediaFileService;

    @Autowired
    private MediaFileMapper mediaFileMapper;

    private MediaFile mediaFile;
    private MediaFileMetadata mediaFileMetadata;

    @BeforeAll
    void setup(){
        mediaFile = TestObjectsBuilder.buildMediaFile(null);
        mediaFileMetadata = TestObjectsBuilder.buildMetadata(null);

        mediaFileService = new MediaFileServiceImpl(metadataStorage, hadoopFileStorage, localFileStorage,
                mediaFileMapper, securityService, metadataEnricher, dtoHandler);
        mediaFileService.setStorageMode("local");

        UserProfile testUser = UserProfile.builder().username("test").id("test").build();
        when(userProfileManager.fetchUserProfile()).thenReturn(testUser);
        dtoHandler = new DTOHandler(userProfileManager);
    }

    @Test
    void shouldGetMediaFile() throws FileStoreServiceException, FileNotAvailableException, FileNotFoundException {
        //given
        MediaFileMetadata mediaFileMetadata = TestObjectsBuilder.buildMetadata(null);
        String mediaFileId = mediaFile.getUuid();

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(Optional.of(mediaFileMetadata));
        //when
        MediaFile result = mediaFileService.getMediaFile(mediaFileId);
        //then
        assertEquals(mediaFile, result);
    }

    @Test
    void shouldThrowException_WhenGetMediaFileWithInvalidId() throws FileStoreServiceException {
        //given
        String invalidId = "invalidId";

        when(metadataStorage.getMetadata(invalidId)).thenReturn(Optional.empty());
        //then
        assertThrowsExactly(FileNotFoundException.class, () -> mediaFileService.getMediaFile(invalidId));
    }

    @Test
    void shouldThrowException_WhenGetInactivatedMediaFile() throws FileStoreServiceException {
        //given
        MediaFileMetadata mediaFileMetadata = TestObjectsBuilder.buildMetadata(null);
        String mediaFileId = mediaFileMetadata.getId();
        mediaFileMetadata.setActivated(false);

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(Optional.of(mediaFileMetadata));
        //then
        assertThrowsExactly(FileNotAvailableException.class, () -> mediaFileService.getMediaFile(mediaFileId));
    }

    @Test
    void shouldAddMediaFileWithoutBytes() throws FileStoreServiceException {
        //given
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(TestObjectsBuilder.buildMetadata(null));
        //when
        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, MediaFileModule.TEXT);
        //then
        assertEquals(mediaFile, savedMediaFile);
    }

    @Test
    void shouldAddMediaFileWithBytes() throws FileStoreServiceException {
        //given
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(eq(mediaFile.getFile()), any(MediaFileMetadata.class))).thenReturn(Optional.of(mediaFileMetadata));
        //when
        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, MediaFileModule.TEXT);
        //then
        assertEquals(mediaFile, savedMediaFile);
    }

    @Test
    void shouldAddMediaGalleryWithBytes() throws FileStoreServiceException {
        //given
        final MediaFile mediaGallery = TestObjectsBuilder.buildMediaGallery(null);
        final MediaFileMetadata mediaFileMetadata = TestObjectsBuilder.buildGalleryMetadata(null);
        MediaFileMetadata child1 = mediaFileMetadata.getChildren().get(0);
        MediaFileMetadata child2 = mediaFileMetadata.getChildren().get(1);

        when(metadataStorage.saveMetadata(mediaFileMetadata)).thenReturn(mediaFileMetadata);
        when(metadataStorage.saveMetadata(child1)).thenReturn(child1);
        when(metadataStorage.saveMetadata(child2)).thenReturn(child2);
        //when
        MediaFile savedMediaFile = mediaFileService.addMediaGallery(mediaGallery);
        //then
        assertEquals(mediaGallery, savedMediaFile);
    }

    @Test
    void shouldUpdateMediaFile() throws IOException, FileStoreServiceException, FileNotFoundException {
        //given
        final String mediaFileId = mediaFile.getUuid();
        final MediaFileMetadata mediaFileMetadata = TestObjectsBuilder.buildMetadata(null);

        when(metadataStorage.getMetadata(mediaFileId)).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(mediaFileId, mediaFileMetadata)).thenReturn(mediaFileMetadata);
        //when
        MediaFile savedMediaFile = mediaFileService.updateMediaFile(mediaFileId, mediaFile, null);
        //then
        assertEquals(mediaFile, savedMediaFile);
    }

    @Test
    void shouldThrowException_WhenUpdateMediaFileWithInvalidId() throws FileStoreServiceException {
        //given
        String invalidId = "invalid_id";

        when(metadataStorage.getMetadata(invalidId)).thenReturn(Optional.empty());
        //then
        assertThrowsExactly(GalleryNotFoundException.class,
                () -> mediaFileService.updateMediaGallery(invalidId, mediaFile));
    }

    @Test
    void shouldGetNull_WhenUpdateNullMediaFile() throws IOException, FileStoreServiceException {
        //given
        final String mediaFileId = mediaFile.getUuid();
        //when
        MediaFile savedGallery = mediaFileService.updateMediaGallery(mediaFileId, null);
        //then
        assertNull(savedGallery);
    }

    @Test
    void shouldUpdateMediaGallery_ByGivenAMediaFileObject() throws IOException, FileStoreServiceException {
        //given
        final MediaFile gallery = TestObjectsBuilder.buildMediaGallery(null);
        final MediaFileMetadata galleryMetadata = TestObjectsBuilder.buildGalleryMetadata(null);
        final String galleryId = gallery.getUuid();

        when(metadataStorage.getMetadata(galleryId)).thenReturn(Optional.of(galleryMetadata));
        when(metadataStorage.updateMetadata(galleryId, galleryMetadata)).thenReturn(galleryMetadata);
        //when
        MediaFile savedGallery = mediaFileService.updateMediaGallery(galleryId, gallery);
        //then
        assertEquals(gallery, savedGallery);
    }

    @Test
    void shouldThrowException_WhenUpdateGalleryWithInvalidId() throws FileStoreServiceException {
        //given
        String invalidId = "invalid_id";
        final MediaFile gallery = TestObjectsBuilder.buildMediaGallery(null);

        when(metadataStorage.getMetadata(invalidId)).thenReturn(Optional.empty());
        //then
        assertThrowsExactly(GalleryNotFoundException.class,
                () -> mediaFileService.updateMediaGallery(invalidId, gallery));
    }

    @Test
    void shouldGetNull_WhenUpdateNullGallery() throws FileStoreServiceException {
        //given
        final MediaFile gallery = TestObjectsBuilder.buildMediaGallery(null);
        final String galleryId = gallery.getUuid();
        //when
        MediaFile savedGallery = mediaFileService.updateMediaGallery(galleryId, null);
        //then
        assertNull(savedGallery);
    }

    @Test
    void shouldGetTrue_WhenUpdateMediaFile() throws IOException, FileStoreServiceException, FileNotFoundException {
        //given
        when(metadataStorage.getMetadata(DUMMY_ID)).thenReturn(Optional.of(mediaFileMetadata));
        when(metadataStorage.updateMetadata(eq(DUMMY_ID), any(MediaFileMetadata.class)))
                .thenReturn(mediaFileMapper.dtoToMetadata(mediaFile));
        //when
        MediaFile updateMediaFile = mediaFileService.updateMediaFile(mediaFile.getUuid(), mediaFile, null);
        //then
        assertNotNull(updateMediaFile);
    }

    @Test
    void shouldGetNewGallery_WhenCloneMediaGallery() throws FileStoreServiceException {
        //given
        final MediaFile gallery = TestObjectsBuilder.buildMediaGallery(null);
        final String galleryId = gallery.getUuid();

        when(metadataStorage.getMetadata(galleryId)).thenReturn(Optional.of(mediaFileMapper.dtoToMetadata(gallery)));
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class)))
                .thenReturn(mediaFileMapper.dtoToMetadata(gallery));
        //when
        MediaFile newGallery = mediaFileService.cloneMediaGallery(galleryId, "test");
        //then
        assertEquals(gallery.getChildren().size(), newGallery.getChildren().size());
    }

}
