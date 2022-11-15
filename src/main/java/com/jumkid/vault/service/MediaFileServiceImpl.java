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

	private final MediaFileSecurityService securityService;

    private final MetadataEnricher metadataEnricher;

	@Autowired
	public MediaFileServiceImpl(FileMetadata<MediaFileMetadata> metadataStorage,
                                FileStorage<MediaFileMetadata> hadoopFileStorage,
                                FileStorage<MediaFileMetadata> localFileStorage,
                                MediaFileMapper mediaFileMapper,
                                MediaFileSecurityService securityService,
                                MetadataEnricher metadataEnricher) {
        this.mediaFileMapper = mediaFileMapper;
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
        Optional<MediaFileMetadata> optional = metadataStorage.getMetadata(mediaFileId);

        if (optional.isEmpty()) throw new FileNotFoundException(mediaFileId);

        MediaFileMetadata metadata = optional.get();

        if (Boolean.TRUE.equals(metadata.getActivated())) return mediaFileMapper.metadataToDto(metadata);
        else throw new FileNotAvailableException();
    }

    @Override
    public MediaFileMetadata getMediaFileMetadata(String mediaFileId) {
        log.debug("Retrieve media file by given id {}", mediaFileId);
        Optional<MediaFileMetadata> optional = metadataStorage.getMetadata(mediaFileId);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            throw new FileNotFoundException(mediaFileId);
        }
    }

    @Override
    public Optional<byte[]> getFileSource(String mediaFileId) {
        log.debug("Retrieve source file by given id {}", mediaFileId);
        Optional<MediaFileMetadata> optional = metadataStorage.getMetadata(mediaFileId);

        if (optional.isPresent()) return getFileStorage().getFileBinary(optional.get());
        else return Optional.empty();
    }

    @Override
    public Optional<byte[]> getThumbnail(String mediaFileId, ThumbnailNamespace thumbnailNamespace) {
        log.debug("Retrieve thumbnail of file by given id {}", mediaFileId);
        Optional<MediaFileMetadata> optional = metadataStorage.getMetadata(mediaFileId);
        if (optional.isPresent() && Boolean.TRUE.equals(optional.get().getActivated())) {
            return getFileStorage().getThumbnail(optional.get(), thumbnailNamespace);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public FileChannel getFileChannel(String mediaFileId) {
        log.debug("Retrieve file channel by given id {}", mediaFileId);
        Optional<MediaFileMetadata> optional = metadataStorage.getMetadata(mediaFileId);

        return optional.map(metadata -> getFileStorage().getFileRandomAccess(metadata).orElseThrow()).orElse(null);
    }

    //TODO make the whole process transactional
    @Override
    public MediaFile addMediaFile(MediaFile mediaFile, MediaFileModule mediaFileModule) {
        normalizeDTO(null, mediaFile, null);

        MediaFileMetadata metadata = mediaFileMapper.dtoToMetadata(mediaFile);
        metadata.setModule(mediaFileModule);
        byte[] file = mediaFile.getFile();
	    if(file == null || file.length == 0) {
            metadata = metadataStorage.saveMetadata(metadata);
        } else {
            metadataEnricher.enrichProps(metadata, file);
            //save metadata to get indexed doc with id
            metadata = metadataStorage.saveMetadata(metadata);
            //save file binary to file system
            Optional<MediaFileMetadata> optional = getFileStorage().saveFile(file, metadata);
            if (optional.isPresent()) {
                MediaFileMetadata savedMetadata = optional.get();
                //update the logical path to metadata
                metadataStorage.updateLogicalPath(savedMetadata.getId(), savedMetadata.getLogicalPath());
            } else {
                log.error("failed to add file {}", metadata);
            }

        }
        return mediaFileMapper.metadataToDto(metadata);
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
                        .mimeType(child.getMimeType())
                        .module(MediaFileModule.REFERENCE)
                        .build());
            }
            galleryMetadata.setChildren(childMetadataList);
            MediaFileMetadata featuredMetadata = childMetadataList.get(0);
            galleryMetadata.addProp(PROP_FEATURED_ID, featuredMetadata.getId());
            // as gallery is not a single file, use featured file mime type as its own
            galleryMetadata.setMimeType(featuredMetadata.getMimeType());
        }
        galleryMetadata = metadataStorage.saveMetadata(galleryMetadata);

        return mediaFileMapper.metadataToDto(galleryMetadata);
    }

    @Override
    public MediaFile updateMediaFile(String mediaFileId, MediaFile partialMediaFile, byte[] bytes) {
        Optional<MediaFileMetadata> optional = metadataStorage.getMetadata(mediaFileId);

        if (optional.isPresent()) {
            MediaFileMetadata updateMetadata = optional.get();
            normalizeDTO(mediaFileId, partialMediaFile, updateMetadata);

            mediaFileMapper.updateMetadataFromDto(partialMediaFile, updateMetadata);

            try {
                if (bytes == null || bytes.length == 0) {

                    metadataStorage.updateMetadata(mediaFileId, updateMetadata);

                } else {

                    Optional<MediaFileMetadata> updated = getFileStorage().saveFile(bytes, updateMetadata);
                    if (updated.isPresent()) {

                        updateMetadata = updated.get();
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

        Optional<MediaFileMetadata> optional = metadataStorage.getMetadata(galleryId);

        if (optional.isPresent()) {
            MediaFileMetadata oldGallery = optional.get();
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

    /**
     *
     * @param mediaFileId media file identity
     * @return number of trashed media file
     */
    @Override
    public Integer trashMediaFile(String mediaFileId) {
       Optional<MediaFileMetadata> optional = metadataStorage.getMetadata(mediaFileId);

        if (optional.isEmpty() || optional.get().getActivated() != Boolean.TRUE) {
            log.warn("metadata is not found for media file {}", mediaFileId);
            return 0;
        }

        MediaFileMetadata metadata = optional.get();
        if (metadata.getModule().equals(MediaFileModule.GALLERY)) {
            return trashGallery(metadata);
        }

        metadataStorage.updateMetadataStatus(mediaFileId, false);

        try {
            getFileStorage().deleteFile(metadata);
            return 1;
        } catch (FileNotFoundException ex) {
            metadataStorage.updateLogicalPath(mediaFileId, null);
        } catch (Exception e) {
            e.printStackTrace();
            //roll back metadata status
            metadataStorage.updateMetadataStatus(mediaFileId, true);
            throw new FileStoreServiceException("failed to trash gallery, please contact system admin");
        }

        return 0;
    }

    private Integer trashGallery(MediaFileMetadata galleryMetadata) {
	    String galleryId = galleryMetadata.getId();
        metadataStorage.updateMetadataStatus(galleryId, false);

        try {
            if (galleryMetadata.getChildren() != null) {
                galleryMetadata.getChildren().forEach(galleryItem -> trashMediaFile(galleryItem.getId()));

                return galleryMetadata.getChildren().size();
            }
        } catch (Exception e) {
            e.printStackTrace();
            metadataStorage.updateMetadataStatus(galleryId, true); //roll back metadata status
            throw new FileStoreServiceException("failed to trash gallery, please contact system admin");
        }
        return 0;
    }

    @Override
    public List<MediaFile> searchMediaFile(String query, Integer size) {
        List<MediaFileMetadata> mediaFileMetadataList = metadataStorage.searchMetadata(query, size,
                securityService.getCurrentUserRoles(), securityService.getCurrentUserId());
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

        String currentUserId = securityService.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        dto.setModificationDate(now);

        if (oldMetadata != null) {
            dto.setModifiedBy(currentUserId);
            dto.setCreatedBy(oldMetadata.getCreatedBy());
            dto.setCreationDate(oldMetadata.getCreationDate());
        } else {
            dto.setCreatedBy(currentUserId);
            dto.setCreationDate(now);
        }

        if (dto.getActivated() == null) dto.setActivated(true);

    }

}
