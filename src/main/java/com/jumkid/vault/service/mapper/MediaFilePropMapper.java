package com.jumkid.vault.service.mapper;

import com.jumkid.vault.controller.dto.Prop;
import com.jumkid.vault.model.MediaFileProp;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface MediaFilePropMapper {

    Prop metadataToDto(MediaFileProp mediaFileProp);

    MediaFileProp dtoToMetadata(Prop prop);

}
