package com.nkeanyi.knowledge.model;

import com.nkeanyi.knowledge.citation.CitationPackage;

public record KnowledgeAnswerResponse(
        String query,
        String answer,
        CitationPackage citationPackage
) {}
