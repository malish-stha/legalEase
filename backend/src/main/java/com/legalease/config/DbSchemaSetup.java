package com.legalease.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbSchemaSetup implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DbSchemaSetup.class);

    private final JdbcTemplate jdbcTemplate;

    public DbSchemaSetup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting database schema setup and pgvector validation...");
        try {
            // 1. Enable pgvector extension
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");
            log.info("pgvector extension verified/created successfully.");

            // 2. Create doc_embeddings table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS doc_embeddings (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    doc_id UUID REFERENCES documents(id) ON DELETE CASCADE,
                    chunk_text TEXT NOT NULL,
                    chunk_index INT NOT NULL,
                    embedding vector(768),
                    created_at TIMESTAMPTZ DEFAULT now()
                );
            """);
            log.info("doc_embeddings table verified/created successfully.");

            // 3. Create IVFFlat cosine similarity index
            try {
                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS doc_embeddings_embedding_idx 
                    ON doc_embeddings USING ivfflat (embedding vector_cosine_ops);
                """);
                log.info("IVFFlat vector index verified/created successfully.");
            } catch (Exception indexEx) {
                log.warn("Could not create IVFFlat index. This can happen if table is empty or memory limits are tight. Skipping index creation...", indexEx);
            }

            // 4. Create conversations table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS conversations (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
                    doc_id UUID REFERENCES documents(id) ON DELETE CASCADE,
                    messages TEXT DEFAULT '[]',
                    created_at TIMESTAMPTZ DEFAULT now(),
                    CONSTRAINT unique_user_doc UNIQUE (user_id, doc_id)
                );
            """);
            log.info("conversations table verified/created successfully.");

        } catch (Exception e) {
            log.error("Fatal error during database schema setup", e);
        }
    }
}
