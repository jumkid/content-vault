package com.jumkid.vault.controller.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jumkid.share.security.AccessScope;
import com.jumkid.share.service.dto.GenericDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vehicle extends GenericDTO {

    private String id;

    private String name;

    private String make;

    private String model;

    private Integer modelYear;

    private AccessScope accessScope;

    private String trimLevel;

    private String mediaGalleryId;

}
