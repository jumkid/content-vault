package com.jumkid.vault.service;

import com.jumkid.vault.controller.dto.MediaFile;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
public class MediaFileServiceImplTest {

    private final int DEFAULT_SIZE = 100;

    @Mock
    private MetadataStorage metadataStorage;
    @Mock
    private HadoopFileStorage hadoopFileStorage;
    @Mock
    private LocalFileStorage localFileStorage;

    private MediaFileServiceImpl mediaFileService;
    private static LocalDateTime now;

    @Before
    public void setup(){
        mediaFileService = new MediaFileServiceImpl(metadataStorage, hadoopFileStorage, localFileStorage);
        mediaFileService.setStorageMode("local");
        now = LocalDateTime.now();
    }

    @Test
    public void shouldAddMediaFileWithoutBytes() {
        final MediaFile mediaFile = generateMediaFile();
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(generateMediaFileMetadata());
        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, null);

        Assertions.assertThat(savedMediaFile).isEqualTo(generateMediaFile());
    }

    @Test
    public void shouldAddMediaFileWithBytes() {
        final MediaFile mediaFile = generateMediaFile();
        final MediaFileMetadata mediaFileMetadata = generateMediaFileMetadata();
        byte[] bytes = new byte[DEFAULT_SIZE];
        when(metadataStorage.saveMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(localFileStorage.saveFile(eq(bytes), any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        when(metadataStorage.updateMetadata(any(MediaFileMetadata.class))).thenReturn(mediaFileMetadata);
        MediaFile savedMediaFile = mediaFileService.addMediaFile(mediaFile, bytes);

        Assertions.assertThat(savedMediaFile).isEqualTo(generateMediaFile());
    }

    private MediaFile generateMediaFile() {
        return MediaFile.builder()
                .title("test").filename("test file").uuid("1")
                .mimeType("plain/text").activated(true)
                .content("test content").size(DEFAULT_SIZE)
                .creationDate(now).modificationDate(now)
                .build();
    }

    private MediaFileMetadata generateMediaFileMetadata() {
        return MediaFileMetadata.builder()
                .title("test").filename("test file").id("1")
                .mimeType("plain/text").activated(true)
                .content("test content").size(DEFAULT_SIZE)
                .module("mfile").logicalPath("/foo")
                .creationDate(now).modificationDate(now)
                .build();
    }

}
