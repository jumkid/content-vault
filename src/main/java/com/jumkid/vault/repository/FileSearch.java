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

import com.jumkid.vault.model.MediaFileMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Created at Sep2018$
 *
 * @author chooliyip
 **/
public interface FileSearch<T> {

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
    T saveMetadata(byte[] bytes, T t);

    /**
     * Get type from repository by given identifier
     *
     * @param id identity of media
     */
    T getMetadata(String id);

    /**
     * Get file from repository
     *
     * @param id identity of media
     */
    Optional<byte[]> getBinary(String id);

    /**
     * Remove file from search
     *
     * @param id identity of media file
     */
    boolean deleteMetadata(String id);

    /**
     * update the metadata of give type
     *
     * @param T type
     * @return type
     */
    T updateMetadata(T t);

    /**
     * Get all media files from repository
     *
     * @return List of type
     */
    List<T> getAll();

    /**
     * Get all trashed metadata from repository
     *
     * @return List of type
     */
    List<T> getTrash();

}
