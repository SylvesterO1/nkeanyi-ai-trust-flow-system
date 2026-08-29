package com.nkeanyi.knowledge.model;

import com.nkeanyi.knowledge.citation.CitationPackage;

public record PackagedKnowledgeResponse(
        KnowledgeQueryResponse retrieval,
        CitationPackage citationPackage
) {}
