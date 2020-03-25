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

import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.util.*;

import com.jumkid.share.util.DateTimeUtils;
import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.exception.FileNotfoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.FileSearch;
import com.jumkid.vault.repository.FileStorage;
import com.jumkid.vault.service.mapper.MediaFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service("fileService")
public class MediaFileServiceImpl implements MediaFileService {

    @Value("${vault.storage.mode}")
    private String storageMode;

	private FileSearch<MediaFileMetadata> fileSearch;

	private final Map<String, FileStorage<MediaFileMetadata>> storageRegistry = new HashMap<>();

	private static final String STORAGE_MODE_LOCAL = "local";
	private static final String STORAGE_MODE_HADOOP = "hadoop";

	private final MediaFileMapper mediaFileMapper = Mappers.getMapper( MediaFileMapper.class );

	@Autowired
	public MediaFileServiceImpl(FileSearch<MediaFileMetadata> esContentStorage,
                                FileStorage<MediaFileMetadata> hadoopFileStorage,
                                FileStorage<MediaFileMetadata> localFileStorage) {
        storageRegistry.put(STORAGE_MODE_LOCAL, localFileStorage);
        storageRegistry.put(STORAGE_MODE_HADOOP, hadoopFileStorage);
	    this.fileSearch = esContentStorage;
	}

	private FileStorage<MediaFileMetadata> getFileStorage() {
	    return storageMode.equals(STORAGE_MODE_LOCAL) ? storageRegistry.get(STORAGE_MODE_LOCAL) : storageRegistry.get(STORAGE_MODE_HADOOP);
    }

    // TODO make the whole process transactional
    @Override
    public MediaFile addMediaFile(MediaFile mediaFile, byte[] bytes) {
        normalizeDTO(null, mediaFile, null);

        MediaFileMetadata mediaFileMetadata = mediaFileMapper.dtoToMetadata(mediaFile);
	    if(bytes == null || bytes.length == 0) {
	        mediaFileMetadata = fileSearch.saveMetadata(mediaFileMetadata);
        } else {
            //firstly save metadata to get indexed doc with id
            mediaFileMetadata = fileSearch.saveMetadata(mediaFileMetadata);
            //second save file binary to file system
            mediaFileMetadata = getFileStorage().saveFile(bytes, mediaFileMetadata);
            //finally update the logical path to metadata
            mediaFileMetadata = fileSearch.updateMetadata(mediaFileMetadata);
        }
        return mediaFileMapper.metadataToDTO(mediaFileMetadata);
    }

    @Override
    public MediaFile updateMediaFile(String uuid, MediaFile mediaFile, byte[] bytes) {
        MediaFileMetadata oldMetadata = fileSearch.getMetadata(uuid);
        if (oldMetadata != null) {
            normalizeDTO(uuid, mediaFile, oldMetadata);

            MediaFileMetadata mediaFileMetadata = mediaFileMapper.dtoToMetadata(mediaFile);
            mediaFileMetadata.setLogicalPath(oldMetadata.getLogicalPath());

            if (bytes == null || bytes.length == 0) {
                if (mediaFileMetadata.getLogicalPath() != null) getFileStorage().deleteFile(mediaFileMetadata);
                mediaFileMetadata.setLogicalPath(null);
                fileSearch.updateMetadata(mediaFileMetadata);
                log.debug("save file metadata. {}", mediaFileMetadata);
            } else {
                mediaFileMetadata = getFileStorage().saveFile(bytes, mediaFileMetadata);
                log.debug("save file binary. {}", mediaFileMetadata);
                mediaFileMetadata = fileSearch.updateMetadata(mediaFileMetadata);
                log.debug("save file metadata. {}", mediaFileMetadata);
            }
            return mediaFileMapper.metadataToDTO(mediaFileMetadata);
        } else {
            throw new FileNotfoundException(uuid);
        }
    }

    @Override
	public MediaFile getMediaFile(String id) {
    	log.debug("Retrieve media file by given id {}", id);
        MediaFileMetadata mediaFileMetadata = fileSearch.getMetadata(id);
        if (mediaFileMetadata != null) {
            return mediaFileMapper.metadataToDTO(mediaFileMetadata);
        } else {
            throw new FileNotfoundException(id);
        }
	}

    @Override
    public MediaFileMetadata getMediaFileMetadata(String id) {
        log.debug("Retrieve media file by given id {}", id);
        MediaFileMetadata mediaFileMetadata = fileSearch.getMetadata(id);
        if (mediaFileMetadata != null) {
            return mediaFileMetadata;
        } else {
            throw new FileNotfoundException(id);
        }
    }

    @Override
    public Optional<byte[]> getFileSource(String id) {
        log.debug("Retrieve source file by given id {}", id);
        MediaFileMetadata mediaFileMetadata = fileSearch.getMetadata(id);
        return getFileStorage().getFileBinary(mediaFileMetadata);
    }

    @Override
    public FileChannel getFileChannel(String id) {
        log.debug("Retrieve file channel by given id {}", id);
        MediaFileMetadata mediaFileMetadata = fileSearch.getMetadata(id);
	    return getFileStorage().getFileRandomAccess(mediaFileMetadata).orElseThrow();
    }

    @Override
    public void deleteMediaFile(String id) {
        MediaFileMetadata mediaFileMetadata = fileSearch.getMetadata(id);
        if(mediaFileMetadata != null) {
            mediaFileMetadata.setActivated(false);
            fileSearch.updateMetadata(mediaFileMetadata);
            try {
                getFileStorage().deleteFile(mediaFileMetadata);
            } catch (Exception e) {
                //roll back metadata status
                mediaFileMetadata.setActivated(true);
                fileSearch.updateMetadata(mediaFileMetadata);
                throw new FileStoreServiceException("failed to delete media file");
            }
        } else {
            throw new FileNotfoundException(id);
        }
    }

    @Override
    public List<MediaFile> getAll() {
        List<MediaFileMetadata> mediaFileMetadataList = fileSearch.getAll();
        if (mediaFileMetadataList == null) return Collections.emptyList();
        else return mediaFileMapper.metadataListToDTOList(mediaFileMetadataList);
    }

    @Override
    public List<MediaFile> getTrash() {
        List<MediaFileMetadata> mediaFileMetadataList = fileSearch.getTrash();
        if (mediaFileMetadataList == null) return Collections.emptyList();
        else return mediaFileMapper.metadataListToDTOList(mediaFileMetadataList);
    }


    private boolean isRandomAccess(MediaFileMetadata mFile){
        return mFile.getMimeType().startsWith("video") || mFile.getMimeType().startsWith("audio");
    }

    private void normalizeDTO(String uuid, MediaFile dto, MediaFileMetadata oldMetadata) {
        dto.setUuid(uuid);

        LocalDateTime now = DateTimeUtils.getCurrentDateTime();
        dto.setModificationDate(now);

        if (oldMetadata != null) {
            dto.setCreatedBy(oldMetadata.getCreatedBy());
            dto.setCreationDate(oldMetadata.getCreationDate());
        } else {
            dto.setCreationDate(now);
        }

        if (dto.getActivated() == null) dto.setActivated(true);

    }

    public void setStorageMode(String storageMode) { this.storageMode = storageMode; }
	
}
