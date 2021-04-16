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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.util.*;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.StorageMode;
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.FileMetadata;
import com.jumkid.vault.repository.FileStorage;
import com.jumkid.vault.service.mapper.MediaFileMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service("fileService")
public class MediaFileServiceImpl implements MediaFileService {

    @Value("${vault.storage.mode}")
    @Setter
    private String storageMode;

	private final FileMetadata<MediaFileMetadata> metadataStorage;

	private final EnumMap<StorageMode, FileStorage<MediaFileMetadata>> storageRegistry = new EnumMap<>(StorageMode.class);

	private final MediaFileMapper mediaFileMapper = Mappers.getMapper( MediaFileMapper.class );

	@Autowired
	public MediaFileServiceImpl(FileMetadata<MediaFileMetadata> metadataStorage,
                                FileStorage<MediaFileMetadata> hadoopFileStorage,
                                FileStorage<MediaFileMetadata> localFileStorage) {
        storageRegistry.put(StorageMode.LOCAL, localFileStorage);
        storageRegistry.put(StorageMode.HADOOP, hadoopFileStorage);
	    this.metadataStorage = metadataStorage;
	}

	private FileStorage<MediaFileMetadata> getFileStorage() {
	    return StorageMode.valueOf(storageMode.toUpperCase()).equals(StorageMode.LOCAL) ? storageRegistry.get(StorageMode.LOCAL) : storageRegistry.get(StorageMode.HADOOP);
    }

    //TODO make the whole process transactional
    @Override
    public MediaFile addMediaFile(MediaFile mediaFile, byte[] bytes) {
        normalizeDTO(null, mediaFile, null);

        MediaFileMetadata mediaFileMetadata = mediaFileMapper.dtoToMetadata(mediaFile);

	    if(bytes == null || bytes.length == 0) {
	        mediaFileMetadata = metadataStorage.saveMetadata(mediaFileMetadata);
        } else {
            enrichMetadata(mediaFileMetadata, bytes);
            //firstly save metadata to get indexed doc with id
            mediaFileMetadata = metadataStorage.saveMetadata(mediaFileMetadata);
            //second save file binary to file system
            Optional<MediaFileMetadata> optional = getFileStorage().saveFile(bytes, mediaFileMetadata);
            if (optional.isPresent()) {
                //finally update the logical path to metadata
                mediaFileMetadata = metadataStorage.updateMetadata(optional.get());
            } else {
                log.error("failed to add file {}", mediaFileMetadata);
            }

        }
        return mediaFileMapper.metadataToDto(mediaFileMetadata);
    }

    @Override
    public MediaFile updateMediaFile(String uuid, MediaFile mediaFile, byte[] bytes) {
        MediaFileMetadata oldMetadata = metadataStorage.getMetadata(uuid);
        if (oldMetadata != null) {
            normalizeDTO(uuid, mediaFile, oldMetadata);

            MediaFileMetadata mediaFileMetadata = mediaFileMapper.dtoToMetadata(mediaFile);
            mediaFileMetadata.setLogicalPath(oldMetadata.getLogicalPath());

            if (bytes == null || bytes.length == 0) {
                if (mediaFileMetadata.getLogicalPath() != null) getFileStorage().deleteFile(mediaFileMetadata);
                mediaFileMetadata.setLogicalPath(null);
                metadataStorage.updateMetadata(mediaFileMetadata);
                log.debug("save file metadata. {}", mediaFileMetadata);
            } else {
                Optional<MediaFileMetadata> optional = getFileStorage().saveFile(bytes, mediaFileMetadata);
                if (optional.isPresent()) {
                    mediaFileMetadata = optional.get();
                    log.debug("saved file binary {}", mediaFileMetadata);
                    mediaFileMetadata = metadataStorage.updateMetadata(mediaFileMetadata);
                    log.debug("saved file metadata {}", mediaFileMetadata);
                } else {
                    log.error("failed to update file {}", mediaFileMetadata);
                }

            }
            return mediaFileMapper.metadataToDto(mediaFileMetadata);
        } else {
            throw new FileNotFoundException(uuid);
        }
    }

    @Override
	public MediaFile getMediaFile(String id) {
    	log.debug("Retrieve media file by given id {}", id);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(id);
        if (mediaFileMetadata != null) {
            return mediaFileMapper.metadataToDto(mediaFileMetadata);
        } else {
            throw new FileNotFoundException(id);
        }
	}

    @Override
    public MediaFileMetadata getMediaFileMetadata(String id) {
        log.debug("Retrieve media file by given id {}", id);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(id);
        if (mediaFileMetadata != null) {
            return mediaFileMetadata;
        } else {
            throw new FileNotFoundException(id);
        }
    }

    @Override
    public Optional<byte[]> getFileSource(String id) {
        log.debug("Retrieve source file by given id {}", id);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(id);

        return getFileStorage().getFileBinary(mediaFileMetadata);
    }

    @Override
    public Optional<byte[]> getThumbnail(String id, ThumbnailNamespace thumbnailNamespace) {
        log.debug("Retrieve thumbnail of file by given id {}", id);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(id);
        if (mediaFileMetadata != null) {
            return getFileStorage().getThumbnail(mediaFileMetadata, thumbnailNamespace);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public FileChannel getFileChannel(String id) {
        log.debug("Retrieve file channel by given id {}", id);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(id);
	    return getFileStorage().getFileRandomAccess(mediaFileMetadata).orElseThrow();
    }

    @Override
    public void deleteMediaFile(String id) {
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(id);
        if(mediaFileMetadata != null) {
            mediaFileMetadata.setActivated(false);
            metadataStorage.updateMetadata(mediaFileMetadata);
            try {
                getFileStorage().deleteFile(mediaFileMetadata);
            } catch (Exception e) {
                //roll back metadata status
                mediaFileMetadata.setActivated(true);
                metadataStorage.updateMetadata(mediaFileMetadata);
                throw new FileStoreServiceException(e.getMessage());
            }
        } else {
            throw new FileNotFoundException(id);
        }
    }

    @Override
    public List<MediaFile> getAll() {
        List<MediaFileMetadata> mediaFileMetadataList = metadataStorage.getAll();
        if (mediaFileMetadataList == null) return Collections.emptyList();
        else return mediaFileMapper.metadataListToDTOList(mediaFileMetadataList);
    }

    @Override
    public List<MediaFile> getTrash() {
        List<MediaFileMetadata> mediaFileMetadataList = metadataStorage.getTrash();
        if (mediaFileMetadataList == null) return Collections.emptyList();
        else return mediaFileMapper.metadataListToDTOList(mediaFileMetadataList);
    }


    private boolean isRandomAccess(MediaFileMetadata mFile){
        return mFile.getMimeType().startsWith("video") || mFile.getMimeType().startsWith("audio");
    }

    private void normalizeDTO(String uuid, MediaFile dto, MediaFileMetadata oldMetadata) {
        dto.setUuid(uuid);

        LocalDateTime now = LocalDateTime.now();
        dto.setModificationDate(now);

        if (oldMetadata != null) {
            dto.setCreatedBy(oldMetadata.getCreatedBy());
            dto.setCreationDate(oldMetadata.getCreationDate());
        } else {
            dto.setCreationDate(now);
        }

        if (dto.getActivated() == null) dto.setActivated(true);

    }

    private void enrichMetadata(MediaFileMetadata mediaFileMetadata, byte[] bytes) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            parser.parse(stream, handler, metadata);
            for(String metaName : metadata.names()) {
                if (metaName.toLowerCase().contains("date")) {
                    log.debug("meta={}", metaName);
                    mediaFileMetadata.addProp(metaName, LocalDateTime.parse(metadata.get(metaName)));
                } else {
                    mediaFileMetadata.addProp(metaName, metadata.get(metaName));
                }
            }
        } catch (Exception e) {
            log.error("Metadata parsing exception {}", e.getMessage());
        }
    }

}
