package com.jumkid.vault.model;

/*
 * This software is written by Jumkid and subject
 * to a contract between Jumkid and its customer.
 *
 * This software stays property of Jumkid unless differing
 * arrangements between Jumkid and its customer apply.
 *
 *
 * (c)2019 Jumkid Innovation All rights reserved.
 */

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jumkid.vault.enums.MediaFilePropType;
import com.jumkid.vault.util.Constants;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(of = {"id"}, callSuper = false)
public class MediaFileMetadata {

	private String id;

	private String filename;

	private String mimeType;
	
	private Integer size;

	@JsonIgnore
	private String module = Constants.MODULE_MFILE;

	private String title;

	private String content;

	@JsonIgnore
	private Boolean activated = true;

	private String logicalPath;

	@JsonIgnore
	private LocalDateTime creationDate;

	@JsonIgnore
	private String createdBy;

	@JsonIgnore
	private LocalDateTime modificationDate;

	@JsonIgnore
	private String modifiedBy;

	private List<MediaFileProp> props;

	private List<String> tags;

	private List<MediaFileMetadata> children;

	public void addProp(String name, String value, String dataType) {
		if (props == null) {
			props = new ArrayList<>();
		}

		if (props.stream().noneMatch(prop -> prop.name.equals(name))) {
			props.add(MediaFileProp.builder().name(name).value(value)
                    .dataType(dataType != null ? MediaFilePropType.fromValue(dataType) : MediaFilePropType.STRING)
                    .build());
		}

	}

}
