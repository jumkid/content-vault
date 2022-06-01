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

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.util.*;

import com.jumkid.vault.controller.dto.MediaFile;
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
import com.jumkid.vault.service.enrich.MetadataEnricher;
import com.jumkid.vault.service.mapper.MediaFileMapper;
import com.jumkid.vault.service.mapper.MediaFilePropMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.jumkid.vault.util.Constants.PROP_FEATURED_ID;

@Slf4j
@Service("fileService")
public class MediaFileServiceImpl implements MediaFileService {

    @Value("${vault.storage.mode}")
    @Setter
    private String storageMode;

	private final FileMetadata<MediaFileMetadata> metadataStorage;

	private final EnumMap<StorageMode, FileStorage<MediaFileMetadata>> storageRegistry = new EnumMap<>(StorageMode.class);

	private final MediaFileMapper mediaFileMapper;
	private final MediaFilePropMapper mediaFilePropMapper;

	private final MediaFileSecurityService securityService;

    private final MetadataEnricher metadataEnricher;

	@Autowired
	public MediaFileServiceImpl(FileMetadata<MediaFileMetadata> metadataStorage,
                                FileStorage<MediaFileMetadata> hadoopFileStorage,
                                FileStorage<MediaFileMetadata> localFileStorage,
                                MediaFileMapper mediaFileMapper,
                                MediaFilePropMapper mediaFilePropMapper,
                                MediaFileSecurityService securityService, MetadataEnricher metadataEnricher) {
        this.mediaFileMapper = mediaFileMapper;
        this.mediaFilePropMapper = mediaFilePropMapper;
        this.securityService = securityService;
        this.metadataEnricher = metadataEnricher;
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
            metadataEnricher.enrichProps(mediaFileMetadata, file);
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
            galleryMetadata.addProp(PROP_FEATURED_ID, childMetadataList.get(0).getId());
        }
        galleryMetadata = metadataStorage.saveMetadata(galleryMetadata);

        return mediaFileMapper.metadataToDto(galleryMetadata);
    }

    @Override
    public MediaFile updateMediaFile(String mediaFileId, MediaFile partialMediaFile, byte[] bytes) {
        MediaFileMetadata updateMetadata = metadataStorage.getMetadata(mediaFileId);

        if (updateMetadata != null) {
            normalizeDTO(mediaFileId, partialMediaFile, updateMetadata);

            mediaFileMapper.updateMetadataFromDto(partialMediaFile, updateMetadata);

            try {
                if (bytes == null || bytes.length == 0) {
                    metadataStorage.updateMetadata(mediaFileId, updateMetadata);
                } else {
                    Optional<MediaFileMetadata> optional = getFileStorage().saveFile(bytes, updateMetadata);
                    if (optional.isPresent()) {
                        updateMetadata = optional.get();
                        log.debug("saved file binary {}", updateMetadata);
                        updateMetadata = metadataStorage.updateMetadata(mediaFileId, updateMetadata);
                        log.debug("saved file metadata {}", updateMetadata);
                    } else {
                        log.error("failed to update file {}", mediaFileId);
                    }

                }
                return mediaFileMapper.metadataToDto(updateMetadata);
            } catch (IOException e) {
                throw new FileStoreServiceException("Failed to update media file with id " + mediaFileId);
            }

        } else {
            throw new FileNotFoundException(mediaFileId);
        }
    }

    @Override
    public MediaFile updateMediaGallery(String galleryId, MediaFile partialMediaGallery) {
        if (partialMediaGallery == null) return null;

        MediaFileMetadata oldGallery = metadataStorage.getMetadata(galleryId);

        if (oldGallery != null) {
            normalizeDTO(galleryId, partialMediaGallery, oldGallery);

            try {
                MediaFileMetadata updatedGallery = metadataStorage.updateMetadata(galleryId,
                        mediaFileMapper.dtoToMetadata(partialMediaGallery));

                return mediaFileMapper.metadataToDto(updatedGallery);
            } catch (IOException e){
                e.printStackTrace();
                log.error("failed to update gallery metadata with id {} ", galleryId);
                throw new FileStoreServiceException("Failed to update gallery metadata. Please contact system admin.");
            }
        } else {
            throw new GalleryNotFoundException(galleryId);
        }
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

}
