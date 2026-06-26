package com.legalease.repository;

import com.legalease.entity.DocEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocEmbeddingRepository extends JpaRepository<DocEmbedding, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO doc_embeddings (id, doc_id, chunk_text, chunk_index, embedding) 
        VALUES (gen_random_uuid(), :docId, :chunkText, :chunkIndex, cast(:embedding as vector))
    """, nativeQuery = true)
    void insertEmbedding(
            @Param("docId") UUID docId,
            @Param("chunkText") String chunkText,
            @Param("chunkIndex") int chunkIndex,
            @Param("embedding") String embedding
    );

    @Query(value = """
        SELECT chunk_text FROM doc_embeddings 
        WHERE doc_id = :docId 
        ORDER BY embedding <=> cast(:queryEmbedding as vector) 
        LIMIT :limit
    """, nativeQuery = true)
    List<String> findSimilarChunks(
            @Param("docId") UUID docId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit
    );

    void deleteByDocumentId(UUID docId);
}
