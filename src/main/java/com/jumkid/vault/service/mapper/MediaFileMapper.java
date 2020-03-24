package com.jumkid.vault.service.mapper;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.model.MediaFileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel="spring")
public interface MediaFileMapper {

    @Mapping(source = "id", target = "uuid")
    MediaFile metadataToDTO(MediaFileMetadata metadata);

    @Mapping(source = "uuid", target = "id")
    MediaFileMetadata dtoToMetadata(MediaFile dto);

    List<MediaFile> metadataListToDTOList(List<MediaFileMetadata> metadataList);

}
