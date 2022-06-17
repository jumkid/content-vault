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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.Optional;
import java.util.UUID;

import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.thumbnail.ThumbnailFileManager;
import com.jumkid.vault.repository.trash.FileTrashManager;
import com.jumkid.vault.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository("localFileStorage")
public class LocalFileStorage implements FileStorage<MediaFileMetadata>{

	private final ThumbnailFileManager thumbnailFileManager;

	private final FilePathManager filePathManager;

	private final FileTrashManager fileTrashManager;

	@Autowired
	public LocalFileStorage(ThumbnailFileManager thumbnailFileManager, FilePathManager filePathManager, FileTrashManager fileTrashManager) {
		this.thumbnailFileManager = thumbnailFileManager;
		this.filePathManager = filePathManager;
		this.fileTrashManager = fileTrashManager;
	}

	@Override
	public Optional<byte[]> getFileBinary(MediaFileMetadata mediaFileMetadata) {
		FileChannel fc = getFileChannel(mediaFileMetadata);
		if (fc != null) {
			return FileUtils.fileChannelToBytes(fc);
		} else {
			return Optional.empty();
		}
	}

	@Override
	public Optional<FileChannel> getFileRandomAccess(MediaFileMetadata mediaFileMetadata) {
		return Optional.ofNullable(this.getRandomAccessFile(mediaFileMetadata));
	}

	@Override
	public Optional<MediaFileMetadata> saveFile(byte[] bytes, MediaFileMetadata mediaFile) {
		
		if(bytes == null) return Optional.empty();

		String logicalPath = filePathManager.getFullPath(mediaFile);

		SeekableByteChannel sbc = null;
		try{
			mediaFile.setLogicalPath(logicalPath);

			Path dirPath = Paths.get(filePathManager.getDataHomePath(), logicalPath);
			Path path = Paths.get(filePathManager.getDataHomePath(), logicalPath, getFileUuid(bytes, mediaFile));

			if(Files.exists(path)){   //replace the existing file if it exists
				sbc = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			}else{
				if(!Files.exists(dirPath)) Files.createDirectories(dirPath);
				sbc = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
			}

			sbc.write(ByteBuffer.wrap(bytes));

			//generate thumbnail for image
			if(mediaFile.getMimeType().startsWith("image/")){
				thumbnailFileManager.generateThumbnail(path);
			}

			return Optional.of(mediaFile);
		} catch (FileAlreadyExistsException fae) {
			log.error("file is already exists. {} ", fae.getMessage());
		} catch (IOException ioe) {
			log.error("failed to write file bytes: {}", ioe.getMessage());
		} catch(Exception e){
			throw new FileStoreServiceException(e.getMessage());
		} finally {
			try {
				assert sbc != null;
				sbc.close();
			} catch (IOException ioe) {
				log.error(ioe.getMessage());
			}
		}
		
		return Optional.empty();
	}

	private FileChannel getFileChannel(MediaFileMetadata mediaFile) {
		if (mediaFile == null || mediaFile.getLogicalPath() == null) return null;

		Path path = Paths.get(filePathManager.getDataHomePath(), mediaFile.getLogicalPath(), mediaFile.getId());

		if (!Files.exists(path)) {
			log.info("file {} is not found.", path);
			return null;
		}

		try {
			FileInputStream fin = new FileInputStream(new File(path.toString()));
			return fin.getChannel();
		} catch (Exception e) {
			throw new FileStoreServiceException(e.getMessage());
			//move to trash if 
		}
	}

	private FileChannel getRandomAccessFile(MediaFileMetadata mediaFile) {

		Path path = Paths.get(filePathManager.getDataHomePath(), mediaFile.getLogicalPath(), mediaFile.getId());

		if(!Files.exists(path)){
			log.info("file {} is not found.", path);
			return null;
		}

		try {
			RandomAccessFile aFile = new RandomAccessFile(path.toString(), "rw");
			return aFile.getChannel();
		} catch (Exception e) {
			throw new FileStoreServiceException(e.getMessage());
			//move to trash if 
		}
	}

	@Override
	public void deleteFile(MediaFileMetadata mediaFile) throws FileNotFoundException, FileStoreServiceException {
		if (mediaFile.getLogicalPath() == null) return;
		Path path = Paths.get(filePathManager.getDataHomePath(), mediaFile.getLogicalPath());
		String mediaFileId = mediaFile.getId();

		if(!Files.exists(path)) {
			log.warn("media file {} is not found", mediaFileId);
			throw new FileNotFoundException(mediaFileId);
		}

		try {
			fileTrashManager.moveToTrash(path, mediaFileId);
		} catch (Exception e) {
			log.error("failed to delete file {} {}", path.toString(), e.getMessage());
			throw new FileStoreServiceException(mediaFileId);
		}
	}

	@Override
	public Optional<byte[]> getThumbnail(MediaFileMetadata mediaFileMetadata, ThumbnailNamespace thumbnailNamespace) {
		return thumbnailFileManager.getThumbnail(mediaFileMetadata, thumbnailNamespace);
	}

	private String getFileUuid(byte[] bytes, MediaFileMetadata mediaFile) {
	    if(mediaFile.getId()==null){
			mediaFile.setId(UUID.nameUUIDFromBytes(bytes).toString());
        }
	    return mediaFile.getId();
    }


    @Override
    public void emptyTrash() {
		log.info("clean up entire trash file store");
		fileTrashManager.emptyTrash();
	}

}
