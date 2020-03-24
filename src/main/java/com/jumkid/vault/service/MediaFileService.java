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
import com.jumkid.vault.model.MediaFileMetadata;

import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Optional;

public interface MediaFileService {

    /**
     * Add new media file and binary
     *
     * @param mediaFile file search info
     * @param file binary of file
     * @return MediaFile
     */
    MediaFile addMediaFile(MediaFile mediaFile, byte[] file);

    /**
     * Update existing media file metadata and binary
     *
     * @param uuid
     * @param mediaFile
     * @param file
     * @return
     */
    MediaFile updateMediaFile(String uuid, MediaFile mediaFile, byte[] file);

    /**
     * Retrieve media file by id
     *
     * @param id
     * @return MediaFile
     */
    MediaFile getMediaFile(String id);

    /**
     * Retrieve media file by id
     *
     * @param id
     * @return MediaFileMetadata
     */
    MediaFileMetadata getMediaFileMetadata(String id);

    /**
     * Retrieve media file source by id
     *
     * @param id media file identity
     * @return FileChannel
     */
    Optional<byte[]> getFileSource(String id);

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
    void deleteMediaFile(String id);

    /**
     * Get all media files
     *
     * @return List of mediaFile
     */
    List<MediaFile> getAll();

}
