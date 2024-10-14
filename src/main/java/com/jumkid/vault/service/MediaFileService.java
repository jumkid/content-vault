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
import com.jumkid.vault.enums.MediaFileModule;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;

import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Optional;

public interface MediaFileService {

    /**
     * Add new media file and binary
     *
     * @param mediaFile media file info
     * @return MediaFile
     */
    MediaFile addMediaFile(MediaFile mediaFile, MediaFileModule mediaFileModule) throws FileStoreServiceException;

    /**
     * Add new media gallery and binaries
     *
     * @param mediaGallery media file for gallery
     * @return MediaFile
     */
    MediaFile addMediaGallery(MediaFile mediaGallery) throws FileStoreServiceException;

    /**
     * Update an existing gallery
     *
     * @param galleryId media gallery identity
     * @param partialMediaGallery media file for gallery
     * @return MediaFile
     */
    MediaFile updateMediaGallery(String galleryId, MediaFile partialMediaGallery) throws FileStoreServiceException;

    /**
     * Clone an existing media gallery to target media gallery by copying its properties and children
     *
     * @param galleryId media gallery identifier
     * @param toGalleryId target media gallery identifier
     * @param title title of media gallery
     * @return MediaFile
     */
    MediaFile cloneMediaGalleryTo(String galleryId, String toGalleryId, String title);

    /**
     * Clone an existing media gallery by copying its properties and children
     *
     * @param galleryId media gallery identifier
     * @param title title of media gallery
     * @return MediaFile
     */
    MediaFile cloneMediaGallery(String galleryId, String title);

    /**
     * Update existing media file metadata and binary
     *
     * @param mediaFileId media file identity
     * @param partialMediaFile partial media file info
     * @param file binary of file
     * @return MediaFile
     */
    MediaFile updateMediaFile(String mediaFileId, MediaFile partialMediaFile, byte[] file) throws FileNotFoundException, FileStoreServiceException;

    /**
     * Retrieve media file by id
     *
     * @param mediaFileId media file identity
     * @return MediaFile
     */
    MediaFile getMediaFile(String mediaFileId) throws FileNotAvailableException, FileNotFoundException, FileStoreServiceException;

    /**
     * Retrieve media file by id
     *
     * @param mediaFileId media file identity
     * @return MediaFileMetadata
     */
    MediaFileMetadata getMediaFileMetadata(String mediaFileId) throws FileNotFoundException, FileStoreServiceException;

    /**
     * Retrieve media file binary by id
     *
     * @param mediaFileId media file identity
     * @return FileChannel
     */
    Optional<byte[]> getFileSource(String mediaFileId) throws FileStoreServiceException;

    /**
     * Get thumbnail of media file by id
     *
     * @param mediaFileId media file identity
     * @return optional of binary
     */
    Optional<byte[]> getThumbnail(String mediaFileId, ThumbnailNamespace thumbnailNamespace) throws FileStoreServiceException;

    /**
     * Retrieve media file source by id
     *
     * @param mediaFileId media file identity
     * @return FileChannel
     */
    FileChannel getFileChannel(String mediaFileId) throws FileStoreServiceException;

    /**
     *
     * @param mediaFileId media file identity
     */
    Integer trashMediaFile(String mediaFileId) throws FileStoreServiceException;

    /**
     * Move gallery items to trash
     *
     * @param galleryId gallery id
     * @param itemsId list of gallery items id
     * @return list of none trashed items
     */
    List<MediaFile> trashMediaGalleryItems(String galleryId, String[] itemsId) throws FileNotFoundException, FileStoreServiceException;

    /**
     * Get all media files
     *
     * @return List of mediaFile
     */
    List<MediaFile> searchMediaFile(String query, Integer size) throws FileStoreServiceException;

    /**
     * Get all trashed media files
     *
     * @return List of mediaFile
     */
    List<MediaFile> getTrash() throws FileStoreServiceException;

    /**
     * Empty the entire trash and clean up file stored in trash
     */
    long emptyTrash() throws FileStoreServiceException;
}
