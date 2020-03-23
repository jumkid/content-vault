package com.jumkid.vault.service.mapper;

import com.jumkid.vault.controller.dto.MediaFile;
import com.jumkid.vault.model.MediaFileMetadata;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel="spring", uses = {MediaFileMapper.class})
public interface ListMediaFileMapper {

    List<MediaFile> metadataListToDTOList(List<MediaFileMetadata> metadataList);

}
