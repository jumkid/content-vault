package com.jumkid.vault.service.mapper;

import com.jumkid.vault.controller.dto.MediaFileProp;
import com.jumkid.vault.model.MediaFilePropMetadata;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface MediaFilePropMapper {

    public MediaFileProp metadataToDto(MediaFilePropMetadata metadata);

    public MediaFilePropMetadata dtoToMetadata(MediaFileProp dto);

}
