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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;

import com.jumkid.vault.enums.ThumbnailInfo;
import com.jumkid.vault.exception.FileNotfoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository("localFileStorage")
public class LocalFileStorage implements FileStorage<MediaFileMetadata>{

	@Value("${vault.data.home}")
	private String dataHomePath;

	@Value("${vault.thumbnail.small}")
	private int thumbnailSmall;

    @Value("${vault.thumbnail.large}")
	private int thumbnailLarge;

	private final FilePathManager filePathManager;

	private final FileTrashManager fileTrashManager;

	@Autowired
	public LocalFileStorage(FilePathManager filePathManager, FileTrashManager fileTrashManager) {
		this.filePathManager = filePathManager;
		this.fileTrashManager = fileTrashManager;
	}

	@Override
	public Optional<byte[]> getFileBinary(MediaFileMetadata mediaFileMetadata) {
		FileChannel fc = getFileChannel(mediaFileMetadata);
		return FileUtils.fileChannelToBytes(fc);
	}

	@Override
	public Optional<FileChannel> getFileRandomAccess(MediaFileMetadata mediaFileMetadata) {
		return Optional.ofNullable(this.getRandomAccessFile(mediaFileMetadata));
	}

	@Override
	public MediaFileMetadata saveFile(byte[] bytes, MediaFileMetadata mediaFile) {
		
		if(bytes==null) return null;

		String logicalPath = filePathManager.getFullPath(mediaFile);

		try{
			mediaFile.setLogicalPath(logicalPath);

			Path dirPath = Paths.get(dataHomePath, logicalPath);
			Path path = Paths.get(dataHomePath, logicalPath, getFileUuid(bytes, mediaFile));

			SeekableByteChannel sbc = null;
			if(Files.exists(path)){   //replace the existing file if it exists
				sbc = Files.newByteChannel(path, StandardOpenOption.WRITE,
						StandardOpenOption.TRUNCATE_EXISTING);
			}else{
				if(!Files.exists(dirPath)) Files.createDirectories(dirPath);
				sbc = Files.newByteChannel(path, StandardOpenOption.WRITE,
						StandardOpenOption.CREATE_NEW);
			}

			try {
				sbc.write(ByteBuffer.wrap(bytes));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally{
				sbc.close();
			}

			//generate thumbnail for image
			if(mediaFile.getMimeType().startsWith("image/")){
				generateThumbnail(path);
			}

			return mediaFile;

		} catch (FileAlreadyExistsException fae) {
			log.error("file is already exists. {} ", fae.getMessage());
		} catch(Exception e){
			throw new FileStoreServiceException(e.getMessage());
		}
		
		return null;
		
	}

	private FileChannel getFileChannel(MediaFileMetadata mediaFile) {

		Path path = Paths.get(dataHomePath, mediaFile.getLogicalPath(), mediaFile.getId());

		if (!Files.exists(path)) {
			log.info("file {} is not found.", path);
			return null;
		}

		try (FileInputStream fin = new FileInputStream(new File(path.toString()))) {
			return fin.getChannel();
		} catch (Exception e) {
			throw new FileStoreServiceException(e.getMessage());
			//move to trash if 
		}
		
	}
	
	/**
	 * Get file from repository with random accessing
	 * 
	 * @param mediaFile
	 * @return
	 * @throws FileStoreServiceException
	 */
	private FileChannel getRandomAccessFile(MediaFileMetadata mediaFile) {

			Path path = Paths.get(dataHomePath, mediaFile.getLogicalPath(), mediaFile.getId());
			
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
	public void deleteFile(MediaFileMetadata mediaFile) {
		if (mediaFile.getLogicalPath() == null) return;

		Path path = Paths.get(dataHomePath, mediaFile.getLogicalPath());
		if(!Files.exists(path)) {
			throw new FileNotfoundException(mediaFile.getId());
		}

		fileTrashManager.moveToTrash(path, mediaFile.getId());
		try {
			FileUtils.deleteDirectoryStream(path);
		} catch (Exception e) {
			log.error("failed to delete file {} {}", path.toString(), e.getMessage());
			throw new FileStoreServiceException(mediaFile.getId());
		}

	}

	@Override
	public Optional<byte[]> getThumbnail(MediaFileMetadata mediaFileMetadata, boolean large) {

		String logicalPath = mediaFileMetadata.getLogicalPath();
		String filePath = null;
		if(mediaFileMetadata.getMimeType().startsWith("image")) {
			filePath = String.format("%s%s/%s%s%s", dataHomePath, logicalPath, mediaFileMetadata.getId(), large ? ThumbnailInfo.LARGE_SUFFIX : ThumbnailInfo.SMALL_SUFFIX, ThumbnailInfo.EXTENSION);
		} else if(mediaFileMetadata.getMimeType().startsWith("video")) {
			filePath = dataHomePath + "/misc/icon_video.png";
		} else if(mediaFileMetadata.getMimeType().startsWith("audio")) {
			filePath = dataHomePath + "/misc/icon_audio.png";
		} else if(mediaFileMetadata.getMimeType().equals("application/pdf")) {
			filePath = dataHomePath + "/misc/icon_pdf.png";
		} else if(mediaFileMetadata.getMimeType().contains("mspowerpoint")) {
			filePath = dataHomePath + "/misc/icon_ppt.png";
		} else if(mediaFileMetadata.getMimeType().contains("msexcel")) {
			filePath = dataHomePath + "/misc/icon_xls.png";
		} else if(mediaFileMetadata.getMimeType().contains("msword")) {
			filePath = dataHomePath + "/misc/icon_doc.png";
		} else if(mediaFileMetadata.getMimeType().contains("avatar")) {
			filePath = dataHomePath + "/misc/icon_avatar.png";
		} else {
			filePath = dataHomePath + "/misc/icon_file.png";
		}

		File file = new File(filePath);
		if(!file.exists()) {
			log.info("file in {} is not found.", filePath);
			return Optional.empty();
		}

		try (FileInputStream fin = new FileInputStream(file)) {
			return FileUtils.fileChannelToBytes(fin.getChannel());
		} catch(Exception e) {
			log.error("Failed to get file on {}", filePath);
		}
		return Optional.empty();
	}
	
	private void generateThumbnail(Path filePath) throws IOException {
		String path = filePath.toString();
		
		Thumbnails.of(new File(path))
				.size(thumbnailSmall, thumbnailSmall)
				.outputFormat("PNG")
				.toFile(new File(path + ThumbnailInfo.SMALL_SUFFIX));
		
		Thumbnails.of(new File(path))
				.size(thumbnailLarge, thumbnailLarge)
				.outputFormat("PNG")
				.toFile(new File(path + ThumbnailInfo.LARGE_SUFFIX));
		
	}
	
	private void deleteThumbnail(MediaFileMetadata mediaFile) {
		
		if(mediaFile.getMimeType().startsWith("image")){
			Path path_s = getThumbnailPath(mediaFile, ThumbnailInfo.SMALL_SUFFIX.value() + ThumbnailInfo.EXTENSION.value());
			Path path_l = getThumbnailPath(mediaFile, ThumbnailInfo.LARGE_SUFFIX.value() + ThumbnailInfo.EXTENSION.value());
			
			try {
				Files.deleteIfExists(path_s);
				Files.deleteIfExists(path_l);
			} catch (IOException e) {
				log.warn("Failed to remove thumbnail. {}", e.getMessage());
			}
			
		}
		
	}
	
	private Path getThumbnailPath(MediaFileMetadata mediaFile, String suffix){
		return Paths.get(dataHomePath, mediaFile.getLogicalPath(), mediaFile.getId() + suffix);
	}

	private String getFileUuid(byte[] bytes, MediaFileMetadata mfile){
	    if(mfile.getId()==null){
            mfile.setId(UUID.nameUUIDFromBytes(bytes).toString());
        }
	    return mfile.getId();
    }

}
