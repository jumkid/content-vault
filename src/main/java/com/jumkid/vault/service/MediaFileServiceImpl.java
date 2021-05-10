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
import java.time.format.DateTimeParseException;
import java.util.*;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.enums.MediaFileField;
import com.jumkid.vault.enums.MediaFileModule;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import static com.jumkid.share.util.Constants.ANONYMOUS_USER;

@Slf4j
@Service("fileService")
public class MediaFileServiceImpl implements MediaFileService {

    @Value("${vault.storage.mode}")
    @Setter
    private String storageMode;

	private final FileMetadata<MediaFileMetadata> metadataStorage;

	private final EnumMap<StorageMode, FileStorage<MediaFileMetadata>> storageRegistry = new EnumMap<>(StorageMode.class);

	private final MediaFileMapper mediaFileMapper = Mappers.getMapper( MediaFileMapper.class );

	private final MediaFileSecurityService securityService;

	@Autowired
	public MediaFileServiceImpl(FileMetadata<MediaFileMetadata> metadataStorage,
                                FileStorage<MediaFileMetadata> hadoopFileStorage,
                                FileStorage<MediaFileMetadata> localFileStorage, MediaFileSecurityService securityService) {
        this.securityService = securityService;
        storageRegistry.put(StorageMode.LOCAL, localFileStorage);
        storageRegistry.put(StorageMode.HADOOP, hadoopFileStorage);
	    this.metadataStorage = metadataStorage;
	}

	private FileStorage<MediaFileMetadata> getFileStorage() {
	    return StorageMode.valueOf(storageMode.toUpperCase()).equals(StorageMode.LOCAL) ? storageRegistry.get(StorageMode.LOCAL) : storageRegistry.get(StorageMode.HADOOP);
    }

    //TODO make the whole process transactional
    @Override
    public MediaFile addMediaFile(MediaFile mediaFile, byte[] bytes, MediaFileModule mediaFileModule) {
        normalizeDTO(null, mediaFile, null);

        MediaFileMetadata mediaFileMetadata = mediaFileMapper.dtoToMetadata(mediaFile);
        mediaFileMetadata.setModule(mediaFileModule);

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

            mediaFileMetadata.setModule(oldMetadata.getModule());
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
    public boolean updateMediaFileFieldValue(String mediaFileId, MediaFileField mediaFileField, Object value) {
        return metadataStorage.updateMetadataFieldValue(mediaFileId, mediaFileField, value);
    }

    @Override
	public MediaFile getMediaFile(String mediaFileId) {
    	log.debug("Retrieve media file by given id {}", mediaFileId);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(mediaFileId);
        if (mediaFileMetadata != null) {
            return mediaFileMapper.metadataToDto(mediaFileMetadata);
        } else {
            throw new FileNotFoundException(mediaFileId);
        }
	}

    @Override
    public MediaFileMetadata getMediaFileMetadata(String mediaFileId) {
        log.debug("Retrieve media file by given id {}", mediaFileId);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(mediaFileId);
        if (mediaFileMetadata != null) {
            return mediaFileMetadata;
        } else {
            throw new FileNotFoundException(mediaFileId);
        }
    }

    @Override
    public Optional<byte[]> getFileSource(String mediaFileId) {
        log.debug("Retrieve source file by given id {}", mediaFileId);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(mediaFileId);

        return getFileStorage().getFileBinary(mediaFileMetadata);
    }

    @Override
    public Optional<byte[]> getThumbnail(String mediaFileId, ThumbnailNamespace thumbnailNamespace) {
        log.debug("Retrieve thumbnail of file by given id {}", mediaFileId);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(mediaFileId);
        if (mediaFileMetadata != null) {
            return getFileStorage().getThumbnail(mediaFileMetadata, thumbnailNamespace);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public FileChannel getFileChannel(String mediaFileId) {
        log.debug("Retrieve file channel by given id {}", mediaFileId);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(mediaFileId);
	    return getFileStorage().getFileRandomAccess(mediaFileMetadata).orElseThrow();
    }

    @Override
    public void trashMediaFile(String mediaFileId) {
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(mediaFileId);
        if (mediaFileMetadata != null && mediaFileMetadata.getActivated()) {
            metadataStorage.updateMetadataStatus(mediaFileId, false);

            try {
                getFileStorage().deleteFile(mediaFileMetadata);
            } catch (FileNotFoundException ex) {
                metadataStorage.updateLogicalPath(mediaFileId, null);
            } catch (Exception e) {
                //roll back metadata status
                metadataStorage.updateMetadataStatus(mediaFileId, true);
                throw new FileStoreServiceException(e.getMessage());
            }

        } else {
            log.warn("metadata is not found for media file {}", mediaFileId);
        }
    }

    @Override
    public List<MediaFile> searchMediaFile(String query, Integer size) {
        List<MediaFileMetadata> mediaFileMetadataList = metadataStorage.searchMetadata(query, size,
                securityService.getCurrentUserRoles(), securityService.getCurrentUserName());
        if (mediaFileMetadataList == null) return Collections.emptyList();
        else return mediaFileMapper.metadataListToDTOList(mediaFileMetadataList);
    }

    @Override
    public List<MediaFile> getTrash() {
        List<MediaFileMetadata> mediaFileMetadataList = metadataStorage.getInactiveMetadata();
        if (mediaFileMetadataList == null) return Collections.emptyList();
        else return mediaFileMapper.metadataListToDTOList(mediaFileMetadataList);
    }

    @Override
    public long emptyTrash() {
        long count = metadataStorage.deleteInactiveMetadata();
        if (count > 0) {
            log.debug("Deleted {} inactive metadata", count);
            getFileStorage().emptyTrash();
        }
        return count;
    }

    private void normalizeDTO(String uuid, MediaFile dto, MediaFileMetadata oldMetadata) {
        dto.setUuid(uuid);
        String currentUser = getCurrentUserName();
        LocalDateTime now = LocalDateTime.now();
        dto.setModificationDate(now);

        if (oldMetadata != null) {
            dto.setModifiedBy(currentUser);
            dto.setCreatedBy(oldMetadata.getCreatedBy());
            dto.setCreationDate(oldMetadata.getCreationDate());
        } else {
            dto.setCreatedBy(currentUser);
            dto.setCreationDate(now);
        }

        if (dto.getActivated() == null) dto.setActivated(true);

    }

    private String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) return ANONYMOUS_USER;

        if (auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            return userDetails.getUsername();
        } else {
            return auth.getPrincipal().toString();
        }
    }

    private void enrichMetadata(MediaFileMetadata mediaFileMetadata, byte[] bytes) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            parser.parse(stream, handler, metadata);
            for (String metaName : metadata.names()) {
                String metaValue = metadata.get(metaName);
                if (metaValue == null || metaValue.isBlank()) continue;

                if (metaName.toLowerCase().contains("date")) {
                    addDatetimeProp(mediaFileMetadata, metaValue, metaName);
                } else {
                    mediaFileMetadata.addProp(metaName, metaValue);
                }
            }
        } catch (Exception e) {
            log.error("Metadata parsing exception {}", e.getMessage());
        }
    }

    private void addDatetimeProp(MediaFileMetadata mediaFileMetadata, String metaValue, String metaName) {
        try {
            mediaFileMetadata.addProp(metaName, LocalDateTime.parse(metaValue));
        } catch (DateTimeParseException ex) {
            log.debug("meta={} {}", metaName, ex.getMessage());
            mediaFileMetadata.addProp(metaName, metaValue);
        }
    }

}
