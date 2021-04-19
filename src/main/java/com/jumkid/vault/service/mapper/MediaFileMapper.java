package com.jumkid.vault.service.mapper;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.model.MediaFileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface MediaFileMapper {

    default MediaFile metadataToDto(MediaFileMetadata metadata) {
        MediaFile mediaFile = MediaFile.builder()
                .uuid(metadata.getId())
                .mimeType(metadata.getMimeType())
                .size(metadata.getSize())
                .title(metadata.getTitle())
                .content(metadata.getContent())
                .filename(metadata.getFilename())
                .activated(metadata.getActivated())
                .creationDate(metadata.getCreationDate())
                .modificationDate(metadata.getModificationDate())
                .createdBy(metadata.getCreatedBy())
                .modifiedBy(metadata.getModifiedBy())
                .build();

        if (metadata.getTags() != null) {
            mediaFile.setTags(metadata.getTags());
        }

        if (metadata.getChildren() != null) {
            List<MediaFile> children = new ArrayList<>();
            for (MediaFileMetadata mediaFileMetadata : metadata.getChildren()) {
                children.add(this.metadataToDto(mediaFileMetadata));
            }
            mediaFile.setChildren(children);
        }

        return mediaFile;
    }

    @Mapping(source = "uuid", target = "id")
    default MediaFileMetadata dtoToMetadata(MediaFile dto) {
        MediaFileMetadata metadata = MediaFileMetadata.builder()
                    .id(dto.getUuid())
                    .mimeType(dto.getMimeType())
                    .size(dto.getSize())
                    .title(dto.getTitle())
                    .content(dto.getContent())
                    .filename(dto.getFilename())
                    .activated(dto.getActivated())
                    .tags(dto.getTags())
                    .creationDate(dto.getCreationDate())
                    .modificationDate(dto.getModificationDate())
                    .createdBy(dto.getCreatedBy())
                    .modifiedBy(dto.getModifiedBy())
                    .build();

        if (dto.getChildren() != null) {
            List<MediaFileMetadata> children = new ArrayList<>();
            int idx = 0;
            for (MediaFile mediaFile : dto.getChildren()) {
                MediaFileMetadata child = this.dtoToMetadata(mediaFile);
                child.setId(String.valueOf(idx++));
                children.add(child);
            }
            metadata.setChildren(children);
        }

        return metadata;
    }

    List<MediaFile> metadataListToDTOList(List<MediaFileMetadata> metadataList);

}
