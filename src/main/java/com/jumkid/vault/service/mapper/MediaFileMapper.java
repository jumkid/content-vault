package com.jumkid.vault.service.mapper;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.model.MediaFileMetadata;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel="spring", uses = {MediaFilePropMapper.class})
public interface MediaFileMapper {

    @Mapping(source = "id", target = "uuid")
    @Mapping(source="metadata.children", target = "children")
    public MediaFile metadataToDto(MediaFileMetadata metadata);

    @Mapping(source = "uuid", target = "id")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public MediaFileMetadata dtoToMetadata(MediaFile dto);

    List<MediaFile> metadataListToDTOList(List<MediaFileMetadata> metadataList);

    @Mapping(target="props", source="partialDto.props")
    @Mapping(target="children", source="partialDto.children")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateMetadataFromDto(MediaFile partialDto, @MappingTarget MediaFileMetadata updateMetadata);

}
