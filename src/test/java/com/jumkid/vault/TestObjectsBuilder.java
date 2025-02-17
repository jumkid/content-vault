package com.jumkid.vault;

import com.jumkid.share.security.AccessScope;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.model.MediaFilePropMetadata;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestObjectsBuilder {

    public static int DEFAULT_SIZE = 100;
    static LocalDateTime now = LocalDateTime.now();

    public static String DUMMY_ID = "dummy-id";

    public static MediaFile buildMediaFile(String uuid) {
        MediaFile mediaFile = MediaFile.builder()
                .uuid(uuid == null ? DUMMY_ID : uuid)
                .title("test.title")
                .filename("upload-test.html")
                .mimeType("plain/text")
                .activated(true)
                .content("test.content")
                .size(DEFAULT_SIZE)
                .createdOn(now).modifiedOn(now)
                .build();

        mediaFile.setFile(new byte[DEFAULT_SIZE]);

        return mediaFile;
    }

    public static MediaFile buildMediaGallery(String mediaGalleryId) {
        MediaFile mediaFile = MediaFile.builder()
                .uuid(mediaGalleryId == null ? DUMMY_ID : mediaGalleryId)
                .title("gallery")
                .filename("test gallery")
                .mimeType("application/octet-stream")
                .module(MediaFileModule.GALLERY)
                .accessScope(AccessScope.PUBLIC)
                .activated(true)
                .content("test.gallery").size(DEFAULT_SIZE)
                .createdOn(now).modifiedOn(now)
                .build();

        List<MediaFile> children = new ArrayList<>();
        children.add(buildMediaFile("1"));
        children.add(buildMediaFile("2"));
        mediaFile.setChildren(children);

        return mediaFile;
    }

    public static MediaFileMetadata buildMetadata(String metadataId) {
        List<MediaFilePropMetadata> props = new ArrayList<>();
        props.add(MediaFilePropMetadata.builder()
                .name("author").textValue("Mr nobody")
                .build());

        return MediaFileMetadata.builder()
                .id(metadataId == null ? DUMMY_ID : metadataId)
                .title("test.title")
                .filename("upload-test.html")
                .mimeType("plain/text")
                .content("test.content")
                .size(DEFAULT_SIZE)
                .activated(true)
                .module(MediaFileModule.TEXT)
                .accessScope(AccessScope.PRIVATE)
                .logicalPath("/foo")
                .props(props)
                .build();
    }

    public static List<MediaFileMetadata> buildListOfMetadata() {
        final List<MediaFileMetadata> metadataLst = new ArrayList<>();
        metadataLst.add(buildMetadata(null));
        metadataLst.add(buildMetadata("dummy-id-1"));
        return metadataLst;
    }

    public static MediaFileMetadata buildGalleryMetadata(String mediaGalleryId) {
        MediaFileMetadata metadata = MediaFileMetadata.builder()
                .id(mediaGalleryId == null ? DUMMY_ID : mediaGalleryId)
                .title("gallery")
                .filename("test gallery")
                .mimeType("application/octet-stream").activated(true)
                .content("test.gallery")
                .size(DEFAULT_SIZE)
                .module(MediaFileModule.GALLERY)
                .accessScope(AccessScope.PUBLIC)
                .tags(List.of("test", "gallery"))
                .createdOn(now)
                .modifiedOn(now)
                .build();

        List<MediaFileMetadata> children = new ArrayList<>();
        children.add(buildMetadata("1"));
        children.add(buildMetadata("2"));
        metadata.setChildren(children);

        return metadata;
    }

}
