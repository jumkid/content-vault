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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jumkid.vault.enums.MediaFileModule;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaFileMetadata {

	private String id;

	private String filename;

	private String mimeType;
	
	private Integer size;

	private MediaFileModule module;

	private String title;

	private String content;

	private Boolean activated = true;

	private String logicalPath;

	private LocalDateTime creationDate;

	private String createdBy;

	private LocalDateTime modificationDate;

	private String modifiedBy;

	private List<MediaFileProp> props;

	private List<String> tags;

	private List<MediaFileMetadata> children;

	public void addProp(String name, String value) {
		if (isPropNotExist(name)) {
			props.add(MediaFileProp.builder().name(name).textValue(value)
                    .build());
		}
	}

	public void addProp(String name, LocalDateTime date) {
		if (isPropNotExist(name)) {
			props.add(MediaFileProp.builder().name(name).dateValue(date)
					.build());
		}
	}

	public void addProp(String name, Integer number) {
		if (isPropNotExist(name)) {
			props.add(MediaFileProp.builder().name(name).numberValue(number)
					.build());
		}
	}

	private boolean isPropNotExist(String propName) {
		if (props == null) {
			props = new ArrayList<>();
			return true;
		} else{
			return props.stream().noneMatch(prop -> prop.getName().equals(propName));
		}
	}

}
