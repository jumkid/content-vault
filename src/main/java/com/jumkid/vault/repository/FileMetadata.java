package com.jumkid.vault.repository;
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

import com.jumkid.vault.enums.MediaFileField;

import java.util.List;
import java.util.Optional;

/**
 * Created at Sep2018$
 *
 * @author chooliyip
 **/
public interface FileMetadata<T> {

    /**
     * Persist file metadata in search repository
     *
     * @param t generic type of file
     */
    T saveMetadata(T t);
    /**
     * Persist file metadata and binary in repository
     *
     * @param bytes file binary
     * @param t generic type of file
     */
    T saveMetadata(T t, byte[] bytes);

    /**
     * Get type from repository by given identifier
     *
     * @param mediaFileId identity of media
     */
    T getMetadata(String mediaFileId);

    /**
     * Get file from repository
     *
     * @param mediaFileId identity of media
     */
    Optional<byte[]> getBinary(String mediaFileId);

    /**
     * Remove file from search
     *
     * @param mediaFileId identity of media file
     */
    boolean deleteMetadata(String mediaFileId);

    /**
     * update the metadata of give type
     *
     * @param t type
     * @return type
     */
    T updateMetadata(T t);

    /**
     * update the metadata of give single field name and value
     *
     * @param mediaFileId identity of media file
     * @param mediaFileField for field name
     * @param value for the field
     * @return true if succeed
     */
    boolean updateMetadataFieldValue(String mediaFileId, MediaFileField mediaFileField, Object value);

    /**
     * update metadata active status
     *
     * @param mediaFileId identity of media file
     */
    void updateMetadataStatus(String mediaFileId, boolean active);

    /**
     * Update metadata file storage path
     *
     * @param mediaFileId identity of media file
     * @param logicalPath file path on the storage
     */
    void updateLogicalPath(String mediaFileId, String logicalPath);

    /**
     * Search media files with given query string
     *
     * @param query query keyword
     * @param size size of result set
     * @param currentUserRole current user roles
     * @param currentUsername current username
     * @return List of type
     */
    List<T> searchMetadata(String query, Integer size, List<String> currentUserRole, String currentUsername);

    /**
     * Get all trashed metadata from repository
     *
     * @return List of type
     */
    List<T> getInactiveMetadata();

    /**
     * Remove all inactive metadata
     */
    long deleteInactiveMetadata();

}
