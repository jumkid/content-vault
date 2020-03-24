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
 *
 */

import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;
import static com.jumkid.vault.util.Constants.*;

import com.jumkid.vault.service.mapper.MediaFileMapper;
import com.jumkid.vault.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;

import static com.jumkid.vault.enums.MediaFileField.*;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
@Repository("esContentStorage")
public class ESContentStorage implements FileSearch<MediaFileMetadata> {

    private final RestHighLevelClient esClient;

    private final MediaFileMapper mediaFileMapper;

    @Autowired
    public ESContentStorage(RestHighLevelClient esClient, MediaFileMapper mediaFileMapper) {
        this.esClient = esClient;
        this.mediaFileMapper = mediaFileMapper;
    }

    @Override
    public MediaFileMetadata saveMetadata(MediaFileMetadata mediaFileMetadata) {
        return saveMetadata(null, mediaFileMetadata);
    }

    @Override
    public MediaFileMetadata saveMetadata(byte[] bytes, MediaFileMetadata mediaFileMetadata) {
        try {
            XContentBuilder builder = XContentFactory.cborBuilder();
            builder.startObject()
                    .field(TITLE.value(), mediaFileMetadata.getTitle())
                    .field(FILENAME.value(), mediaFileMetadata.getFilename())
                    .field(SIZE.value(), mediaFileMetadata.getSize())
                    .field(MODULE.value(), mediaFileMetadata.getModule())
                    .field(MIMETYPE.value(), mediaFileMetadata.getMimeType())
                    .field(CONTENT.value(), mediaFileMetadata.getContent())
                    .field(ACTIVATED.value(), mediaFileMetadata.getActivated())
                    .timeField(CREATION_DATE.value(), mediaFileMetadata.getCreationDate())
                    .field(CREATED_BY.value(), mediaFileMetadata.getCreatedBy())
                    .timeField(MODIFICATION_DATE.value(), mediaFileMetadata.getModificationDate())
                    .field(MODIFIED_BY.value(), mediaFileMetadata.getModifiedBy())
                    .field(BLOB.value(), bytes)
                    .endObject();
            IndexRequest request = new IndexRequest(MODULE_MFILE).source(builder);
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            request.opType(DocWriteRequest.OpType.CREATE);
//            request.setPipeline("pipeline");

            //Synchronous execution
            IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);
            mediaFileMetadata.setId(response.getId());

            return mediaFileMetadata;
        } catch (IOException ioe) {
            log.error("failed to create index {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to save media file into Elasticsearch, please contact system administrator.", mediaFileMapper.metadataToDTO(mediaFileMetadata));
        }
    }

    @Override
    public MediaFileMetadata getMetadata(String id) {

        GetRequest request = new GetRequest(MODULE_MFILE).id(id);

        try {
            GetResponse getResponse = esClient.get(request, RequestOptions.DEFAULT);
            if(!getResponse.isExists()) {
                return null;
            }

            return MediaFileMetadata.builder()
                    .id(getResponse.getId())
                    .createdBy((String)getResponse.getSource().get(CREATED_BY.value()))
                    .creationDate(DateTimeUtils.stringToLocalDatetime((String)getResponse.getSource().get(CREATION_DATE.value())))
                    .modifiedBy((String)getResponse.getSource().get(MODIFIED_BY.value()))
                    .modificationDate(DateTimeUtils.stringToLocalDatetime((String)getResponse.getSource().get(CREATION_DATE.value())))
                    .mimeType((String)getResponse.getSource().get(MIMETYPE.value()))
                    .module((String)getResponse.getSource().get(MODULE.value()))
                    .content((String)getResponse.getSource().get(CONTENT.value()))
                    .activated((Boolean)getResponse.getSource().get(ACTIVATED.value()))
                    .filename((String)getResponse.getSource().get(FILENAME.value()))
                    .title((String)getResponse.getSource().get(TITLE.value()))
                    .size((Integer)getResponse.getSource().get(SIZE.value()))
                    .logicalPath((String)getResponse.getSource().get(LOGICALPATH.value()))
                    .build();
        } catch (IOException ioe) {
            log.error("failed to get media file {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to get media file from Elasticsearch, please contact system administrator.");
        }

    }

    @Override
    public Optional<byte[]> getBinary(String id) {
        GetRequest request = new GetRequest(MODULE_MFILE).id(id);
        try {
            GetResponse getResponse = esClient.get(request, RequestOptions.DEFAULT);

            //TODO investigate why ES returns base64 encoded field value
            byte[] bytes = Base64.getDecoder().decode((String)getResponse.getSource().get(BLOB.value()));
            return Optional.of(bytes);
        } catch (IOException ioe) {
            log.error("failed to get source file {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to get media file from Elasticsearch, please contact system administrator.");
        }
    }

    @Override
    public boolean deleteMetadata(String id) {
        DeleteRequest deleteRequest = new DeleteRequest(MODULE_MFILE).id(id);
        deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        try {
            DeleteResponse deleteResponse = esClient.delete(deleteRequest, RequestOptions.DEFAULT);
            return deleteResponse.getResult() == DocWriteResponse.Result.DELETED;
        } catch (IOException ioe) {
            log.error("failed to delete media file {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to delete media file id[" + id + "] from Elasticsearch, please contact system administrator.");
        }
    }

    @Override
    public MediaFileMetadata updateMetadata(MediaFileMetadata mediaFileMetadata) {
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(MODULE_MFILE);
        updateRequest.id(mediaFileMetadata.getId());

        try {
            updateRequest.doc(jsonBuilder()
                    .startObject()
                    .field(TITLE.value(), mediaFileMetadata.getTitle())
                    .field(FILENAME.value(), mediaFileMetadata.getFilename())
                    .field(CONTENT.value(), mediaFileMetadata.getContent())
                    .field(MIMETYPE.value(), mediaFileMetadata.getMimeType())
                    .field(SIZE.value(), mediaFileMetadata.getSize())
                    .field(LOGICALPATH.value(), mediaFileMetadata.getLogicalPath())
                    .endObject());

            esClient.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("failed to update metadata {}", e.getMessage());
        }

        return mediaFileMetadata;
    }

    @Override
    public List<MediaFileMetadata> getAll() {
        List<MediaFileMetadata> results = new ArrayList<>();

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.searchType(SearchType.DEFAULT);

        try {
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            response.getHits().iterator().forEachRemaining( hitDoc -> {
                Map<String, Object> sourceMap = hitDoc.getSourceAsMap();
                MediaFileMetadata mfile = MediaFileMetadata.builder()
                        .id(hitDoc.getId())
                        .title(sourceMap.get(TITLE.value()) !=null ? sourceMap.get(TITLE.value()).toString() : null)
                        .filename(sourceMap.get(FILENAME.value()) !=null ? sourceMap.get(FILENAME.value()).toString() : null)
                        .mimeType(sourceMap.get(MIMETYPE.value()) !=null ? sourceMap.get(MIMETYPE.value()).toString() : null)
                        .size(sourceMap.get(SIZE.value()) != null ? (Integer)sourceMap.get(SIZE.value()) : null)
                        .creationDate(sourceMap.get(CREATION_DATE.value()) != null ? DateTimeUtils.stringToLocalDatetime(sourceMap.get(CREATION_DATE.value()).toString()) : null)
                        .createdBy(sourceMap.get(CREATED_BY.value()) != null ? sourceMap.get(CREATED_BY.value()).toString() : null)
                        .activated(sourceMap.get(ACTIVATED.value()) != null ? (Boolean) sourceMap.get(ACTIVATED.value()) : Boolean.FALSE)
                        .content(sourceMap.get(CONTENT.value()) !=null ? sourceMap.get(CONTENT.value()).toString() : null)
                        .build();
                results.add(mfile);
            });

        } catch (IOException ioe) {
            log.error("failed to search media files: {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to fetch all media files from Elasticsearch, please contact system administrator.");
        }

        return results;
    }

}
