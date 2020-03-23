package com.jumkid.vault.controller.dto;

import com.jumkid.share.controller.dto.GenericDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(of = {"uuid"}, callSuper = false)
public class MediaFile extends GenericDTO {

    private String uuid;

    private String filename;

    private String mimeType;

    private Integer size;

    private String title;

    private String content;

    private Boolean activated;

}
