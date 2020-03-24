package com.jumkid.vault.repository;

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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.jumkid.vault.enums.SystemDirectoryName;
import com.jumkid.vault.model.MediaFileMetadata;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FilePathManager {

	static final String DELIMITER = "/";

	private static final String FORMAT_YYYYMMDD = "yyyyMMdd";

	@Getter
	@Value("${vault.data.home}")
	private String dataHomePath;

	String getTrashPath() {
		return DELIMITER + SystemDirectoryName.TRASH.value();
	}

	/**
	 * Use media file metadata to generate full storage path
	 *
	 * @param mediaFileMetadata the metadata of media file
	 * @return file full path
	 */
	public String getFullPath(MediaFileMetadata mediaFileMetadata) {
		if(mediaFileMetadata.getLogicalPath()!=null) {
			return mediaFileMetadata.getLogicalPath() + DELIMITER + mediaFileMetadata.getId();
		}
		return this.getCategoryPath(mediaFileMetadata.getMimeType()) + getDatePath() + DELIMITER + mediaFileMetadata.getId();
	}

	private String getDatePath(){
		//generate yyyyMMdd string for today
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_YYYYMMDD);
		return DELIMITER + now.format(formatter);
	}
	
	private String getCategoryPath(String mimeType){
		return DELIMITER + mimeType.substring( 0, mimeType.indexOf(DELIMITER) );
	}

}
