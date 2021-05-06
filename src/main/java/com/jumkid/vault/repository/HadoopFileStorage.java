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
import com.jumkid.vault.enums.ThumbnailNamespace;
import com.jumkid.vault.model.MediaFileMetadata;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Optional;

@Slf4j
@Repository
@org.springframework.context.annotation.Configuration
public class HadoopFileStorage implements FileStorage<MediaFileMetadata> {

    @Value("${vault.data.home}")
    private String defaultStorePath;

    private final Configuration conf;

    private final FilePathManager filePathManager;

    @Autowired
    public HadoopFileStorage(FilePathManager filePathManager,
                             @Value("${hdfs.namenode.host}") String nameNodeHost,
                             @Value("${hdfs.namenode.port}") int nameNodePort) {
        this.filePathManager = filePathManager;

        conf = new Configuration();
        String hdfsUri = "hdfs://" + nameNodeHost + ":" + nameNodePort;

        conf.set("fs.defaultFS", hdfsUri);
        conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

        //TODO - do a simple HDFS status check
    }

    @Override
    public Optional<MediaFileMetadata> saveFile(byte[] bytes, MediaFileMetadata mediaFileMetadata) {
        FSDataOutputStream outputStream = null;
        try (FileSystem fs = FileSystem.get(conf)) {
            //get file full path for the media file
            String filePath = defaultStorePath + filePathManager.getFullPath(mediaFileMetadata);
            String folderPath = filePath.substring(0, filePath.lastIndexOf(FilePathManager.DELIMITER));

            Path newFolderPath= new Path(folderPath);
            if(!fs.exists(newFolderPath)) {
                // Create new Directory
                fs.mkdirs(newFolderPath);
                log.info("Path {} created.", folderPath);
            }

            //---- write file
            log.info("write file into hdfs start");
            //Create a path
            Path writePath = new Path(filePath);
            //Init output stream
            outputStream = fs.create(writePath);
            //Cassical output stream usage
            outputStream.write(bytes);
            log.info("write file into hdfs ended");

            mediaFileMetadata.setLogicalPath(folderPath);

        } catch (Exception e) {
            log.error("Failed to save media file {}", e.getMessage());
        } finally {
            try {
                if(outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ioe) {
                log.error("Failed to save media file {}", ioe.getMessage());
            }
        }

        return Optional.of(mediaFileMetadata);
    }

    @Override
    public Optional<byte[]> getFileBinary(MediaFileMetadata mediaFileMetadata) {
        return Optional.empty();
    }

    @Override
    public Optional<FileChannel> getFileRandomAccess(MediaFileMetadata mediaFileMetadata) {
        return Optional.empty();
    }

    @Override
    public void deleteFile(MediaFileMetadata mediaFileMetadata) {
        try (FileSystem fs = FileSystem.get(conf)) {

            Path path = new Path(mediaFileMetadata.getLogicalPath());
            if(!fs.exists(path)) {
                log.error("the path of target file to be deleted does not exist");
            }

            fs.delete(path, true);

        } catch (IOException ioe) {
            log.error("Failed to delete media file {}", ioe.getMessage());
        }
    }

    @Override
    public Optional<byte[]> getThumbnail(MediaFileMetadata mediaFileMetadata, ThumbnailNamespace thumbnailNamespace) {
        return Optional.empty();
    }

    @Override
    public void emptyTrash() {
        //void
    }
}
