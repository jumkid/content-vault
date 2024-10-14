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
import java.nio.channels.FileChannel;
import java.util.Optional;

import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;


public interface FileStorage<T> {

	/**
	 * Persist file in repository
	 * 
	 * @param bytes, t
	 * @param t
	 */
	Optional<T> saveFile(byte[] bytes, T t) throws FileStoreServiceException;

	/**
	 * Get file from repository
	 *
	 * @param mediaFileMetadata metadata of media
	 * @return FileChannel
	 * @throws FileStoreServiceException exception of media storage service
	 */
	Optional<byte[]> getFileBinary(MediaFileMetadata mediaFileMetadata) throws FileStoreServiceException;
	
	/**
	 * Get file from repository with the ability of random access
	 * 
	 * @param mediaFileMetadata metadata of media
	 * @throws FileStoreServiceException exception of media storage service
	 */
	Optional<FileChannel> getFileRandomAccess(MediaFileMetadata mediaFileMetadata) throws FileStoreServiceException;

	/**
	 * Remove file from storage
	 *
	 * @param t metadata
	 * @throws FileStoreServiceException exception of media storage service
	 */
	void deleteFile(T t) throws FileNotFoundException, FileStoreServiceException;

	/**
	 * Get file thumbnail from repository
	 *
	 * @param t metadata
	 * @param thumbnailNamespace thumbnail size option
	 * @throws FileStoreServiceException exception of media storage service
	 */
	Optional<byte[]> getThumbnail(T t, ThumbnailNamespace thumbnailNamespace) throws FileStoreServiceException;

	/**
	 * Clean up file stored in trash
	 */
	void emptyTrash() throws FileStoreServiceException;
}
