package com.nkeanyi.documentintelligence.model;

import java.util.List;

public record DocumentReviewTasksResponse(
        List<DocumentReviewTaskResponse> tasks,
        int count
) {}
