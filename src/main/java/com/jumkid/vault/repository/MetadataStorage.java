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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jumkid.vault.enums.MediaFilePropField;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;
import static com.jumkid.vault.util.Constants.*;

import com.jumkid.vault.model.MediaFileProp;
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
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static com.jumkid.vault.enums.MediaFileField.*;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Slf4j
@Repository("metadataStorage")
public class MetadataStorage implements FileMetadata<MediaFileMetadata> {

    private final RestHighLevelClient esClient;

    private final MediaFileMapper mediaFileMapper;

    @Autowired
    public MetadataStorage(RestHighLevelClient esClient, MediaFileMapper mediaFileMapper) {
        this.esClient = esClient;
        this.mediaFileMapper = mediaFileMapper;
    }

    @Override
    public List<MediaFileMetadata> searchMetadata(String query, Integer size) {
        SearchRequest searchRequest = new SearchRequest();

        searchRequest.searchType(SearchType.DEFAULT);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        QueryBuilder booleanQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(ACTIVATED.value(), true))
                .must(QueryBuilders.simpleQueryStringQuery(query));

        sourceBuilder.size(size == null ? 50 : size);
        sourceBuilder.query(booleanQuery);
        searchRequest.source(sourceBuilder);

        return searchMetadata(searchRequest);
    }

    @Override
    public List<MediaFileMetadata> getInactiveMetadata() {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.searchType(SearchType.DEFAULT);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery(ACTIVATED.value(), false));
        searchRequest.source(sourceBuilder);

        return searchMetadata(searchRequest);
    }

    @Override
    public long deleteInactiveMetadata() {

        DeleteByQueryRequest deleteRequest = new DeleteByQueryRequest(ES_INDEX_MFILE);
        deleteRequest.setConflicts("proceed");
        try {
            deleteRequest.setQuery(new TermQueryBuilder(ACTIVATED.value(), false));
            //can be parallel using sliced-scroll:
            deleteRequest.setSlices(2);
            //uses the scroll parameter to control how long it keeps the "search context" alive.
            deleteRequest.setScroll(TimeValue.timeValueMinutes(10));
            deleteRequest.setRefresh(true);

            BulkByScrollResponse bulkResponse = esClient.deleteByQuery(deleteRequest, RequestOptions.DEFAULT);
            return bulkResponse.getDeleted();
        } catch (IOException ioe) {
            log.error("failed to delete inactive metadata due to {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to delete inactive media file from Elasticsearch, please contact system administrator.");
        }
    }

    private List<MediaFileMetadata> searchMetadata(SearchRequest searchRequest) {
        List<MediaFileMetadata> results = new ArrayList<>();
        try {
            SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
            response.getHits().iterator().forEachRemaining( hitDoc -> {
                MediaFileMetadata mediaFileMetadata = sourceToMetadata(hitDoc.getSourceAsMap());
                mediaFileMetadata.setId(hitDoc.getId());
                results.add(mediaFileMetadata);
            });

        } catch (IOException ioe) {
            log.error("failed to search media files: {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to fetch all media files from Elasticsearch, please contact system administrator.");
        }

        return results;
    }

    @Override
    public MediaFileMetadata getMetadata(String id) {

        GetRequest request = new GetRequest(ES_INDEX_MFILE).id(id);

        try {
            GetResponse getResponse = esClient.get(request, RequestOptions.DEFAULT);
            if(!getResponse.isExists()) {
                return null;
            }

            MediaFileMetadata mediaFileMetadata = sourceToMetadataWithProps(getResponse.getSource());
            mediaFileMetadata.setId(getResponse.getId());
            return mediaFileMetadata;
        } catch (IOException ioe) {
            log.error("failed to get media file {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to get media file from Elasticsearch, please contact system administrator.");
        }

    }

    private MediaFileMetadata sourceToMetadata(Map<String, Object> sourceMap) {
        return MediaFileMetadata.builder()
                .title(sourceMap.get(TITLE.value()) !=null ? sourceMap.get(TITLE.value()).toString() : null)
                .filename(sourceMap.get(FILENAME.value()) !=null ? sourceMap.get(FILENAME.value()).toString() : null)
                .mimeType(sourceMap.get(MIME_TYPE.value()) !=null ? sourceMap.get(MIME_TYPE.value()).toString() : null)
                .size(sourceMap.get(SIZE.value()) != null ? (Integer)sourceMap.get(SIZE.value()) : null)
                .content(sourceMap.get(CONTENT.value()) !=null ? sourceMap.get(CONTENT.value()).toString() : null)
                .logicalPath((String)sourceMap.get(LOGICAL_PATH.value()))
                .activated(sourceMap.get(ACTIVATED.value()) != null ? (Boolean) sourceMap.get(ACTIVATED.value()) : Boolean.FALSE)
                .tags((List<String>) sourceMap.get(TAGS.value()))
                .children(linkToReferencedMetadata((List<MediaFileMetadata>) sourceMap.get(CHILDREN.value())))
                .module((String)sourceMap.get(MODULE.value()))
                .creationDate(sourceMap.get(CREATION_DATE.value()) != null ? DateTimeUtils.stringToLocalDatetime(sourceMap.get(CREATION_DATE.value()).toString()) : null)
                .createdBy(sourceMap.get(CREATED_BY.value()) != null ? sourceMap.get(CREATED_BY.value()).toString() : null)
                .modifiedBy((String)sourceMap.get(MODIFIED_BY.value()))
                .modificationDate(DateTimeUtils.stringToLocalDatetime((String)sourceMap.get(CREATION_DATE.value())))
                .build();
    }

    private MediaFileMetadata sourceToMetadataWithProps(Map<String, Object> sourceMap) {
        MediaFileMetadata mediaFileMetadata = sourceToMetadata(sourceMap);

        if (sourceMap.get(PROPS.value()) != null) {
            List<HashMap<String, Object>> propsLst = (List<HashMap<String, Object>>)sourceMap.get(PROPS.value());
            List<MediaFileProp> props = new ArrayList<>();
            for (HashMap<String, Object> propsMap : propsLst) {
                Object textObj = propsMap.get(MediaFilePropField.TEXT_VALUE.value());
                String textValue = textObj != null ? (String)textObj : null;

                Object dateObj = propsMap.get(MediaFilePropField.DATE_VALUE.value());
                LocalDateTime dateValue = dateObj != null ? DateTimeUtils.stringToLocalDatetime((String)dateObj) : null;

                Object numberObj = propsMap.get(MediaFilePropField.NUMBER_VALUE.value());
                Integer numberValue = numberObj != null ? (Integer)numberObj : null;

                props.add(MediaFileProp.builder()
                        .name((String)propsMap.get(MediaFilePropField.NAME.value()))
                        .textValue(textValue)
                        .dateValue(dateValue)
                        .numberValue(numberValue)
                        .build());
            }
            mediaFileMetadata.setProps(props);
        }

        return mediaFileMetadata;
    }

    private List<MediaFileMetadata> linkToReferencedMetadata(List<MediaFileMetadata> mediaFileMetadataList) {
        if (mediaFileMetadataList == null) return Collections.emptyList();
        List<MediaFileMetadata>  newMetadataList = new ArrayList<>();
        for (MediaFileMetadata mediaFileMetadata : mediaFileMetadataList) {
            String childId = mediaFileMetadata.getId();
            MediaFileMetadata metadata = this.getMetadata(childId);
            newMetadataList.add(Objects.requireNonNullElse(metadata, mediaFileMetadata));
        }
        return newMetadataList;
    }

    @Override
    public MediaFileMetadata saveMetadata(MediaFileMetadata mediaFileMetadata) {
        return saveMetadata(mediaFileMetadata, null);
    }

    @Override
    public MediaFileMetadata saveMetadata(MediaFileMetadata mediaFileMetadata, byte[] bytes) {
        try {
            XContentBuilder builder = XContentFactory.cborBuilder();
            buildContent(builder, mediaFileMetadata);

            IndexRequest request = new IndexRequest(ES_INDEX_MFILE).source(builder);
            request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            //Synchronous execution
            IndexResponse response = esClient.index(request, RequestOptions.DEFAULT);
            mediaFileMetadata.setId(response.getId());

            return mediaFileMetadata;
        } catch (IOException ioe) {
            log.error("failed to create index {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to save media file into Elasticsearch, please contact system administrator.", mediaFileMapper.metadataToDto(mediaFileMetadata));
        }
    }

    @Override
    public MediaFileMetadata updateMetadata(MediaFileMetadata mediaFileMetadata) {
        String id = mediaFileMetadata.getId();
        try {
            XContentBuilder builder = jsonBuilder();
            buildContent(builder, mediaFileMetadata);

            updateMetadata(id, builder);
        } catch (IOException e) {
            log.error("failed to update metadata {} due to {}", id, e.getMessage());
        }

        return mediaFileMetadata;
    }

    @Override
    public void updateMetadataStatus(String id, boolean active) {
        try {
            updateMetadata(id, jsonBuilder().startObject().field(ACTIVATED.value(), active).endObject());
        } catch (IOException e) {
            log.error("failed to update metadata logical path {} due to {}", id, e.getMessage());
        }
    }

    @Override
    public void updateLogicalPath(String id, String logicalPath) {
        try {
            updateMetadata(id, jsonBuilder().startObject().field(LOGICAL_PATH.value(), logicalPath).endObject());
        } catch (IOException e) {
            log.error("failed to update metadata logical path {} due to {}", id, e.getMessage());
        }
    }

    private void updateMetadata(String id, XContentBuilder builder) throws IOException{
        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index(ES_INDEX_MFILE);
        updateRequest.id(id);

        updateRequest.doc(builder);

        esClient.update(updateRequest, RequestOptions.DEFAULT);
    }

    private void buildContent(XContentBuilder builder, MediaFileMetadata mediaFileMetadata){
        try {
            builder.startObject()
                    .field(TITLE.value(), mediaFileMetadata.getTitle())
                    .field(FILENAME.value(), mediaFileMetadata.getFilename())
                    .field(SIZE.value(), mediaFileMetadata.getSize())
                    .field(MODULE.value(), mediaFileMetadata.getModule())
                    .field(MIME_TYPE.value(), mediaFileMetadata.getMimeType())
                    .field(CONTENT.value(), mediaFileMetadata.getContent())
                    .field(LOGICAL_PATH.value(), mediaFileMetadata.getLogicalPath())
                    .field(ACTIVATED.value(), mediaFileMetadata.getActivated());

            if (mediaFileMetadata.getTags() != null) builder.array(TAGS.value(), mediaFileMetadata.getTags().toArray());

            builder.startArray(PROPS.value());
            buildProps(builder, mediaFileMetadata.getProps());
            builder.endArray();

            builder.startArray(CHILDREN.value());
            buildChildren(builder, mediaFileMetadata.getChildren());
            builder.endArray()
                    .timeField(CREATION_DATE.value(), mediaFileMetadata.getCreationDate())
                    .field(CREATED_BY.value(), mediaFileMetadata.getCreatedBy())
                    .timeField(MODIFICATION_DATE.value(), mediaFileMetadata.getModificationDate())
                    .field(MODIFIED_BY.value(), mediaFileMetadata.getModifiedBy())
                    .endObject();
        } catch (IOException e) {
            log.error("failed to build metadata {}", e.getMessage());
        }
    }

    private void buildProps(XContentBuilder builder, List<MediaFileProp> props) throws IOException{
        if (props != null) {
            for (MediaFileProp prop : props) {
                builder.startObject().field(MediaFilePropField.NAME.value(), prop.getName());

                if (prop.getTextValue() != null) builder.field(MediaFilePropField.TEXT_VALUE.value(), prop.getTextValue());
                if (prop.getDateValue() != null) builder.field(MediaFilePropField.DATE_VALUE.value(), prop.getDateValue());
                if (prop.getNumberValue() != null) builder.field(MediaFilePropField.NUMBER_VALUE.value(), prop.getNumberValue());

                builder.endObject();
            }
        }
    }

    private void buildChildren(XContentBuilder builder, List<MediaFileMetadata> children) throws IOException{
        if (children != null) {
            XContentParser parser;
            for (MediaFileMetadata metadata : children) {
                parser = JsonXContent.jsonXContent
                        .createParser(NamedXContentRegistry.EMPTY, LoggingDeprecationHandler.INSTANCE,
                                new ObjectMapper().writeValueAsString(metadata));
                builder.copyCurrentStructure(parser);
            }
        }
    }

    @Override
    public Optional<byte[]> getBinary(String id) {
        GetRequest request = new GetRequest(ES_INDEX_MFILE).id(id);
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
        DeleteRequest deleteRequest = new DeleteRequest(ES_INDEX_MFILE).id(id);
        deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        try {
            DeleteResponse deleteResponse = esClient.delete(deleteRequest, RequestOptions.DEFAULT);
            return deleteResponse.getResult() == DocWriteResponse.Result.DELETED;
        } catch (IOException ioe) {
            log.error("failed to delete media file {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to delete media file id[" + id + "] from Elasticsearch, please contact system administrator.");
        }
    }

}
