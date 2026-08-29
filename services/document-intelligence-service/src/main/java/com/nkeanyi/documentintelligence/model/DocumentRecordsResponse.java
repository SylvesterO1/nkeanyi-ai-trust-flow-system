package com.nkeanyi.documentintelligence.model;

import java.util.List;

public record DocumentRecordsResponse(
        List<DocumentRecordResponse> records,
        int count
) {}
