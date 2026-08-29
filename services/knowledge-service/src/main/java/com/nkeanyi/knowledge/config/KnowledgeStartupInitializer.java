package com.nkeanyi.knowledge.config;

import com.nkeanyi.knowledge.qdrant.QdrantRestClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeStartupInitializer {

    private final QdrantRestClient qdrantRestClient;

    public KnowledgeStartupInitializer(QdrantRestClient qdrantRestClient) {
        this.qdrantRestClient = qdrantRestClient;
    }

    @PostConstruct
    public void init() {
        qdrantRestClient.ensureCollection();
    }
}
