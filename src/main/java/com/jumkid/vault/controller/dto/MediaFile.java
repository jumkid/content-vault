package com.jumkid.vault.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.*;
import com.jumkid.share.controller.dto.GenericDTO;
import com.jumkid.vault.enums.MediaFileModule;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@Data
@EqualsAndHashCode(of = {"uuid", "title"}, callSuper = false)
@JsonInclude(Include.NON_NULL)
public class MediaFile extends GenericDTO {

    private String uuid;

    private String filename;

    private String mimeType;

    private Integer size;

    @NotBlank
    private String title;

    private String content;

    private Boolean activated;

    private MediaFileModule module;

    @JsonIgnore
    private byte[] file;

    private List<String> tags;

    private List<MediaFile> children;

    private List<MediaFileProp> props;

    /**
     * This constructor is for lombok builder only since it is subclass of generic DTO
     *
     */
    @Builder
    public MediaFile(String uuid, String filename, String mimeType, Integer size, String title, String content,
                     Boolean activated, List<String> tags, MediaFileModule module,
                     List<MediaFileProp> props, List<MediaFile> children,
                     String createdBy, LocalDateTime creationDate, String modifiedBy, LocalDateTime modificationDate) {
        super(createdBy, creationDate, modifiedBy, modificationDate);

        this.uuid = uuid;
        this.title = title;
        this.filename = filename;
        this.mimeType = mimeType;
        this.size = size;
        this.content = content;
        this.activated = activated;
        this.tags = tags;
        this.module = module;
        this.props = props;
        this.children = children;
    }
}
