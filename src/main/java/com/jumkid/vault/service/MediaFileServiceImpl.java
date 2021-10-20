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
import com.jumkid.vault.exception.FileNotAvailableException;
import com.jumkid.vault.exception.FileNotFoundException;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.exception.GalleryNotFoundException;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.repository.FileMetadata;
import com.jumkid.vault.repository.FileStorage;
import com.jumkid.vault.service.mapper.MediaFileMapper;
import com.jumkid.vault.util.DateTimeUtils;
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

	private final MediaFileSecurityService securityService;

	private static final String WHITESPACE = "\\t";

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

    @Override
    public MediaFile getMediaFile(String mediaFileId) {
        log.debug("Retrieve media file by given id {}", mediaFileId);
        MediaFileMetadata mediaFileMetadata = metadataStorage.getMetadata(mediaFileId);
        if (mediaFileMetadata == null) throw new FileNotFoundException(mediaFileId);
        else if (Boolean.TRUE.equals(mediaFileMetadata.getActivated())) return mediaFileMapper.metadataToDto(mediaFileMetadata);
        else throw new FileNotAvailableException();
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

    //TODO make the whole process transactional
    @Override
    public MediaFile addMediaFile(MediaFile mediaFile, MediaFileModule mediaFileModule) {
        normalizeDTO(null, mediaFile, null);

        MediaFileMetadata mediaFileMetadata = mediaFileMapper.dtoToMetadata(mediaFile);
        mediaFileMetadata.setModule(mediaFileModule);
        byte[] file = mediaFile.getFile();
	    if(file == null || file.length == 0) {
	        mediaFileMetadata = metadataStorage.saveMetadata(mediaFileMetadata);
        } else {
            enrichMetadata(mediaFileMetadata, file);
            //save metadata to get indexed doc with id
            mediaFileMetadata = metadataStorage.saveMetadata(mediaFileMetadata);
            //save file binary to file system
            Optional<MediaFileMetadata> optional = getFileStorage().saveFile(file, mediaFileMetadata);
            if (optional.isPresent()) {
                MediaFileMetadata savedMetadata = optional.get();
                //update the logical path to metadata
                metadataStorage.updateLogicalPath(savedMetadata.getId(), savedMetadata.getLogicalPath());
            } else {
                log.error("failed to add file {}", mediaFileMetadata);
            }

        }
        return mediaFileMapper.metadataToDto(mediaFileMetadata);
    }

    @Override
    public MediaFile addMediaGallery(MediaFile mediaGallery) {
        normalizeDTO(null, mediaGallery, null);

        MediaFileMetadata galleryMetadata = mediaFileMapper.dtoToMetadata(mediaGallery);
        galleryMetadata.setModule(MediaFileModule.GALLERY);

        if(galleryMetadata.getChildren() != null && !galleryMetadata.getChildren().isEmpty()) {
            List<MediaFileMetadata> childMetadataList = new ArrayList<>();
            for (MediaFile child : mediaGallery.getChildren()) {
                child = this.addMediaFile(child, MediaFileModule.FILE);
                log.debug("save one file {} in new gallery", child.getUuid());
                childMetadataList.add(MediaFileMetadata.builder()
                        .id(child.getUuid())
                        .module(MediaFileModule.REFERENCE)
                        .build());
            }
            galleryMetadata.setChildren(childMetadataList);
        }
        galleryMetadata = metadataStorage.saveMetadata(galleryMetadata);

        return mediaFileMapper.metadataToDto(galleryMetadata);
    }

    @Override
    public MediaFile updateMediaFile(String mediaFileId, MediaFile mediaFile, byte[] bytes) {
        MediaFileMetadata oldMetadata = metadataStorage.getMetadata(mediaFileId);
        if (oldMetadata != null) {
            normalizeDTO(mediaFileId, mediaFile, oldMetadata);

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
            throw new FileNotFoundException(mediaFileId);
        }
    }

    @Override
    public MediaFile updateMediaGallery(String galleryId, MediaFile mediaGallery) {
        if (mediaGallery == null) return null;
        MediaFileMetadata existGallery = metadataStorage.getMetadata(galleryId);
        if (existGallery != null) {
            normalizeDTO(galleryId, mediaGallery, existGallery);

            MediaFileMetadata galleryMetadata = mediaFileMapper.dtoToMetadata(mediaGallery);

            galleryMetadata.setModule(existGallery.getModule());

            List<MediaFile> child = mediaGallery.getChildren();
            if (child != null && !child.isEmpty()) {
                List<MediaFileMetadata> childMetadata = new ArrayList<>();
                for (MediaFile mediaFile : child) {
                    childMetadata.add(MediaFileMetadata.builder()
                            .id(mediaFile.getUuid())
                            .module(MediaFileModule.REFERENCE)
                            .build());
                }
                galleryMetadata.setChildren(childMetadata);
            }

            galleryMetadata = metadataStorage.saveMetadata(galleryMetadata);

            return mediaFileMapper.metadataToDto(galleryMetadata);
        } else {
            throw new GalleryNotFoundException(galleryId);
        }
    }

    @Override
    public boolean updateMediaFileField(String mediaFileId, MediaFileField mediaFileField, Object value) {
	    if (mediaFileId == null || mediaFileId.isBlank() || mediaFileField == null) return false;
        else return metadataStorage.updateMetadataField(mediaFileId, mediaFileField, value);
    }

    @Override
    public boolean updateMediaFileFields(String mediaFileId, Map<MediaFileField, Object> fieldValueMap) {
        if (mediaFileId == null || fieldValueMap == null || fieldValueMap.isEmpty()) return false;
	    else {
            if (fieldValueMap.containsKey(MediaFileField.CHILDREN)) {
                List<MediaFile> children = (List<MediaFile>)fieldValueMap.get(MediaFileField.CHILDREN);
                List<MediaFileMetadata> childrenMetadata = new ArrayList<>();
                for (MediaFile child : children) {
                    if (child.getUuid() == null && child.getFile() != null) { //store the newly upload files
                        child = this.addMediaFile(child, MediaFileModule.FILE);
                        addChildMetadata(childrenMetadata, child);
                    } else if (child.getUuid() != null && this.getMediaFile(child.getUuid()) != null) {
                        addChildMetadata(childrenMetadata, child);
                    }
                    log.debug("saved new child file {}", child.getUuid());
                }
                fieldValueMap.put(MediaFileField.CHILDREN, childrenMetadata);
            }
            return metadataStorage.updateMultipleMetadataFields(mediaFileId, fieldValueMap);
        }
    }

    private void addChildMetadata(List<MediaFileMetadata> childrenMetadata, MediaFile child) {
        childrenMetadata.add(MediaFileMetadata.builder()
                .id(child.getUuid()).module(MediaFileModule.REFERENCE)
                .build());
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
        if (uuid != null) dto.setUuid(uuid);

        String currentUser = securityService.getCurrentUserName();
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

    private void enrichMetadata(MediaFileMetadata mediaFileMetadata, byte[] bytes) {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            parser.parse(stream, handler, metadata);
            for (String metaName : metadata.names()) {
                String metaValue = metadata.get(metaName);
                if (metaValue == null || metaValue.isBlank()) continue;

                if (metaName.toLowerCase().contains("date") || metaName.toLowerCase().contains("modified")) {
                    addDatetimeProp(mediaFileMetadata, metaValue, metaName);
                } else {
                    mediaFileMetadata.addProp(metaName, metaValue + WHITESPACE);  //add whitespace here to escape date type in ES
                }
            }
        } catch (Exception e) {
            log.error("Metadata parsing exception {}", e.getMessage());
        }
    }

    private void addDatetimeProp(MediaFileMetadata mediaFileMetadata, String metaValue, String metaName) {
        try {
            mediaFileMetadata.addProp(metaName, DateTimeUtils.stringToLocalDatetime(metaValue));
        } catch (DateTimeParseException ex) {
            log.error("meta={}  |  {}", metaName, ex.getMessage());
            mediaFileMetadata.addProp(metaName, metaValue + WHITESPACE); //add whitespace here to escape date type in ES
        }
    }

}
