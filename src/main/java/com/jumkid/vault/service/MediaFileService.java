package com.jumkid.vault.service;

/*
 * This software is written by Jumkid and subject
 * to a contract between Jumkid and its customer.
 *
 * This software stays property of Jumkid unless differing
 * arrangements between Jumkid and its customer apply.
 *
 *
 * (c)2019 Jumkid Innovation All rights reserved.
 */

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.model.MediaFileMetadata;

import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Optional;

public interface MediaFileService {

    /**
     * Add new media file and binary
     *
     * @param mediaFile media file info
     * @param file binary of file
     * @return MediaFile
     */
    MediaFile addMediaFile(MediaFile mediaFile, byte[] file);

    /**
     * Update existing media file metadata and binary
     *
     * @param id media file identity
     * @param mediaFile media file info
     * @param file binary of file
     * @return MediaFile
     */
    MediaFile updateMediaFile(String id, MediaFile mediaFile, byte[] file);

    /**
     * Retrieve media file by id
     *
     * @param id media file identity
     * @return MediaFile
     */
    MediaFile getMediaFile(String id);

    /**
     * Retrieve media file by id
     *
     * @param id media file identity
     * @return MediaFileMetadata
     */
    MediaFileMetadata getMediaFileMetadata(String id);

    /**
     * Retrieve media file binary by id
     *
     * @param id media file identity
     * @return FileChannel
     */
    Optional<byte[]> getFileSource(String id);

    /**
     * Get thumbnail of media file by id
     *
     * @param id media file identity
     * @return optional of binary
     */
    Optional<byte[]> getThumbnail(String id, ThumbnailNamespace thumbnailNamespace);

    /**
     * Retrieve media file source by id
     *
     * @param id media file identity
     * @return FileChannel
     */
    FileChannel getFileChannel(String id);

    /**
     *
     * @param id media file identity
     */
    void trashMediaFile(String id);

    /**
     * Get all media files
     *
     * @return List of mediaFile
     */
    List<MediaFile> searchMediaFile(String query, Integer size);

    /**
     * Get all trashed media files
     *
     * @return List of mediaFile
     */
    List<MediaFile> getTrash();

    /**
     * Empty the entire trash and clean up file stored in trash
     */
    long emptyTrash();
}
