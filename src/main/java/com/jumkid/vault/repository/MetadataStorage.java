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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.jumkid.vault.exception.FileStoreServiceException;
import com.jumkid.vault.model.MediaFileMetadata;

import static com.jumkid.share.util.Constants.ADMIN_ROLE;
import static com.jumkid.vault.util.Constants.*;

import com.jumkid.vault.service.mapper.MediaFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;

import static com.jumkid.vault.enums.MediaFileField.*;

@Slf4j
@Repository("metadataStorage")
public class MetadataStorage implements FileMetadata<MediaFileMetadata> {

    private static final String ES_IDX_ENDPOINT = "mfile";

    private final ElasticsearchClient esClient;

    private final MediaFileMapper mediaFileMapper;

    @Autowired
    public MetadataStorage(ElasticsearchClient esClient, MediaFileMapper mediaFileMapper) {
        this.esClient = esClient;
        this.mediaFileMapper = mediaFileMapper;
    }

    @Override
    public List<MediaFileMetadata> searchMetadata(String query, Integer size,
                                                  List<String> currentUserRole, String currentUsername) {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                    .index(ES_INDEX_MFILE)
                    .size(size == null ? 50 : size);

        BoolQuery.Builder booleanQueryBuilder = new BoolQuery.Builder()
                .filter(tq -> tq.term(t -> t.field(ACTIVATED.value()).value(true)))
                .must(m -> m.simpleQueryString(sq -> sq.query(query)));

        if (!currentUserRole.contains(ADMIN_ROLE)) {
            booleanQueryBuilder.filter(tq -> tq.term(t -> t.field(CREATED_BY.value()).value(currentUsername)));
        }

        searchRequestBuilder.query(booleanQueryBuilder.build()._toQuery());

        try {
            SearchResponse<MediaFileMetadata> response = esClient.search(searchRequestBuilder.build(), MediaFileMetadata.class);

            return searchMetadata(response);
        } catch (IOException ioe) {
            log.error("failed to search metadata due to {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to search media file in Elasticsearch, please contact system administrator.");
        }


    }

    @Override
    public List<MediaFileMetadata> getInactiveMetadata() {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(ES_IDX_ENDPOINT)
                .query(q -> q.term(new TermQuery.Builder()
                        .field(ACTIVATED.value()).value(false)
                        .build()))
                .build();

        try {
            SearchResponse<MediaFileMetadata> response = esClient.search(searchRequest, MediaFileMetadata.class);
            return searchMetadata(response);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("failed to search metadata {} ", e.getMessage());
            throw new FileStoreServiceException("Not able to search metadata, please contact system administrator.");
        }
    }

    @Override
    public Long deleteInactiveMetadata() {
        DeleteByQueryRequest deleteRequest = new DeleteByQueryRequest.Builder()
                .index(ES_INDEX_MFILE)
                .query(q -> q.term(new TermQuery.Builder()
                        .field(ACTIVATED.value()).value(false)
                        .build()))
                .conflicts(Conflicts.Proceed)
                .slices(2L)
                .refresh(true)
                .build();

        try {
            DeleteByQueryResponse response = esClient.deleteByQuery(deleteRequest);
            return response.deleted();
        } catch (IOException ioe) {
            log.error("failed to delete inactive metadata due to {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to delete inactive media file from Elasticsearch, please contact system administrator.");
        }
    }

    private List<MediaFileMetadata> searchMetadata(SearchResponse<MediaFileMetadata> response) {
        List<MediaFileMetadata> results = new ArrayList<>();
        response.hits().hits().iterator().forEachRemaining( hitDoc -> addResult(hitDoc, results));
        return results;
    }

    private void addResult(Hit<MediaFileMetadata> hitDoc, List<MediaFileMetadata> results){
        MediaFileMetadata mediaFileMetadata = hitDoc.source();
        if (mediaFileMetadata != null) {
            mediaFileMetadata.setId(hitDoc.id());
            results.add(mediaFileMetadata);
        }
    }

    @Override
    public MediaFileMetadata getMetadata(String mediaFileId) {

        GetRequest request = new GetRequest.Builder()
                .index(ES_INDEX_MFILE)
                .id(mediaFileId)
                .build();

        try {
            GetResponse<MediaFileMetadata> response = esClient.get(request, MediaFileMetadata.class);
            if(response.source() == null) { return null; }

            response.source().setId(mediaFileId);

            return response.source();
        } catch (IOException ioe) {
            log.error("failed to get media file {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to get media file from Elasticsearch, please contact system administrator.");
        }

    }


    @Override
    public MediaFileMetadata saveMetadata(MediaFileMetadata mediaFileMetadata) {
        IndexRequest<MediaFileMetadata> request = new IndexRequest.Builder<MediaFileMetadata>()
                .index(ES_INDEX_MFILE)
                .document(mediaFileMetadata)
                .refresh(Refresh.True)
                .build();

        try {
            IndexResponse response = esClient.index(request);
            mediaFileMetadata.setId(response.id());

            return mediaFileMetadata;
        } catch (IOException ioe) {
            log.error("failed to save metadata {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to save media file into Elasticsearch, please contact system administrator.", mediaFileMapper.metadataToDto(mediaFileMetadata));
        }
    }

    @Override
    public void updateMetadataStatus(String mediaFileId, boolean active) {
        try {
            updateMetadata(mediaFileId,  MediaFileMetadata.builder().activated(active).build());
        } catch (IOException e) {
            log.error("failed to update metadata logical path {} due to {}", mediaFileId, e.getMessage());
        }
    }

    @Override
    public void updateLogicalPath(String mediaFileId, String logicalPath) {
        try {
            updateMetadata(mediaFileId, MediaFileMetadata.builder().logicalPath(logicalPath).build());
        } catch (IOException e) {
            log.error("failed to update metadata logical path {} due to {}", mediaFileId, e.getMessage());
        }
    }

    @Override
    public MediaFileMetadata updateMetadata(String mediaFileId, MediaFileMetadata partialMetadata) throws IOException{
        UpdateRequest<MediaFileMetadata, MediaFileMetadata> updateRequest =
                new UpdateRequest.Builder<MediaFileMetadata, MediaFileMetadata>()
                        .index(ES_INDEX_MFILE)
                        .refresh(Refresh.True)
                        .doc(partialMetadata)
                        .id(mediaFileId)
                        .build();

        UpdateResponse<MediaFileMetadata> response = esClient.update(updateRequest, MediaFileMetadata.class);
        log.info("Updated media file with id {} ", mediaFileId);

        if (response.get() != null) {
            return response.get().source();
        } else {
            return null;
        }

    }

    @Override
    public Optional<byte[]> getBinary(String mediaFileId) {
        GetRequest request = new GetRequest.Builder()
                .index(ES_INDEX_MFILE)
                .id(mediaFileId)
                .build();

        try {
            GetResponse<String> response = esClient.get(request, String.class);

            //TODO investigate why ES returns base64 encoded field value
            byte[] bytes = Base64.getDecoder().decode(response.source());
            return Optional.of(bytes);
        } catch (IOException ioe) {
            log.error("failed to get source file {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to get media file from Elasticsearch, please contact system administrator.");
        }
    }

    @Override
    public boolean deleteMetadata(String mediaFileId) {
        DeleteRequest deleteRequest = new DeleteRequest.Builder()
                .index(ES_INDEX_MFILE)
                .id(mediaFileId)
                .refresh(Refresh.True)
                .build();

        try {
            DeleteResponse deleteResponse = esClient.delete(deleteRequest);
            return deleteResponse.result().equals(Result.Deleted);
        } catch (IOException ioe) {
            log.error("failed to delete media file {} ", ioe.getMessage());
            throw new FileStoreServiceException("Not able to delete media file id[" + mediaFileId + "] from Elasticsearch, please contact system administrator.");
        }
    }

}
