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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.jumkid.vault.enums.MediaFileModule;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.jumkid.share.util.Constants.YYYYMMDDTHHMMSS3S;

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

	private List<MediaFilePropMetadata> props;

	private List<String> tags;

	private List<MediaFileMetadata> children;

	private String createdBy;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = YYYYMMDDTHHMMSS3S)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime creationDate;

	private String modifiedBy;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = YYYYMMDDTHHMMSS3S)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime modificationDate;

	public void addProp(String name, String value) {
		if (isPropNotExist(name)) {
			props.add(MediaFilePropMetadata.builder()
					.name(name).textValue(value)
                    .build());
		}
	}

	public void addProp(String name, LocalDateTime date) {
		if (isPropNotExist(name)) {
			props.add(MediaFilePropMetadata.builder()
					.name(name).dateValue(date)
					.build());
		}
	}

	public void addProp(String name, Number number) {
		if (isPropNotExist(name)) {
			props.add(MediaFilePropMetadata.builder()
					.name(name).numberValue(number)
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
