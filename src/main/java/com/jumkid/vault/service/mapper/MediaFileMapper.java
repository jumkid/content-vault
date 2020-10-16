package com.jumkid.vault.service.mapper;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.controller.dto.Prop;
import com.jumkid.vault.model.MediaFileMetadata;
import com.jumkid.vault.model.MediaFileProp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mapper(uses = MediaFilePropMapper.class)
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
                .tags(metadata.getTags())
                .build();

        if (metadata.getProps() != null) {
            List<Prop> props = new ArrayList<>();
            for (MediaFileProp prop : metadata.getProps()) {
                props.add(Prop.builder()
                        .name(prop.getName())
                        .value(prop.getValue())
                        .dataType(prop.getDataType())
                        .build());
            }
            mediaFile.setProps(props);
        }

        return mediaFile;
    }

    @Mapping(source = "uuid", target = "id")
    MediaFileMetadata dtoToMetadata(MediaFile dto);

    List<MediaFile> metadataListToDTOList(List<MediaFileMetadata> metadataList);

}
