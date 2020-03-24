package com.jumkid.vault.controller.dto;

import com.jumkid.share.controller.dto.GenericDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = {"uuid"}, callSuper = false)
public class MediaFile extends GenericDTO {

    private String uuid;

    private String filename;

    private String mimeType;

    private Integer size;

    private String title;

    private String content;

    private Boolean activated;

    /**
     * This constructor is for lombok builder only since it is subclass of generic DTO
     *
     */
    @Builder
    public MediaFile(String uuid, String filename, String mimeType, Integer size, String title, String content, Boolean activated,
                     String createdBy, LocalDateTime creationDate, String modifiedBy, LocalDateTime modificationDate) {
        super(createdBy, creationDate, modifiedBy, modificationDate);
        this.uuid = uuid;
        this.title = title;
        this.filename = filename;
        this.mimeType = mimeType;
        this.size = size;
        this.content = content;
        this.activated = activated;
    }
}
