export type RequestOptions = {
  tenantId?: string;
  correlationId?: string;
  idempotencyKey?: string;
  authorizationToken?: string;
};

export type HealthResponse = {
  status: string;
  components?: Record<string, unknown>;
};

export type EvidenceRecord = {
  id?: string;
  tenantId?: string;
  correlationId?: string;
  paymentId?: string;
  documentId?: string;
  evidenceType?: string;
  sourceService?: string;
  sourceTopic?: string;
  decision?: string;
  riskLevel?: string;
  createdAt?: string;
  timestamp?: string;
  metadata?: Record<string, unknown>;
  [key: string]: unknown;
};

export type EvidenceSearchResponse = {
  records?: EvidenceRecord[];
  evidence?: EvidenceRecord[];
  items?: EvidenceRecord[];
  data?: EvidenceRecord[] | EvidenceRecord | Record<string, unknown>;
  [key: string]: unknown;
};

export type CreatePaymentRequest = {
  tenantId?: string;
  correlationId?: string;
  customerId: string;
  amount: number;
  currency: string;
  paymentReference: string;
  idempotencyKey?: string;
};

export type PaymentResponse = {
  id?: string;
  paymentId?: string;
  tenantId?: string;
  correlationId?: string;
  status?: string;
  decision?: string;
  amount?: number;
  currency?: string;
  paymentReference?: string;
  createdAt?: string;
  timestamp?: string;
  [key: string]: unknown;
};

export type DocumentProcessResponse = {
  objectName?: string;
  bucket?: string;
  documentId?: string;
  extractedText?: string;
  extractedTextLength?: number;
  extractorUsed?: string;
  ocrUsed?: boolean;
  extractionMode?: string;
  confidence?: number;
  eventStatus?: string;
  status?: string;
  [key: string]: unknown;
};

export type DocumentRecord = {
  documentId: string;
  tenantId?: string;
  correlationId?: string;
  originalFilename?: string;
  contentType?: string;
  extractorUsed?: string;
  confidence?: number;
  status?: string;
  errorMessage?: string | null;
  createdAt?: string;
  updatedAt?: string;
  extractedText?: string | null;
};

export type DocumentRecordsResponse = {
  records: DocumentRecord[];
  count: number;
};

export type DocumentWorkflow = {
  documentId: string;
  tenantId?: string;
  workspaceId?: string;
  correlationId?: string;
  sourceUri?: string;
  checksum?: string;
  ownerId?: string;
  originalFilename?: string;
  contentType?: string;
  documentType?: string;
  sensitivityLabel?: string;
  accessPolicy?: string;
  lifecycleState?: string;
  reviewStatus?: string;
  reviewerId?: string;
  reviewDecision?: string;
  decisionRationale?: string;
  quarantineReason?: string;
  replacementDocumentId?: string;
  retentionPolicy?: string;
  qualityGateStatus?: string;
  qualityGateFindings?: string[];
  extractionConfidence?: number;
  ocrUsed?: boolean;
  extractorUsed?: string;
  publishedAt?: string;
  deprecatedAt?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type DocumentReviewRequest = {
  reviewerId: string;
  decision: string;
  rationale?: string;
  ownerId?: string;
  retentionPolicy?: string;
  replacementDocumentId?: string;
};

export type DocumentReviewTask = {
  id?: string;
  documentId: string;
  tenantId?: string;
  status?: string;
  reason?: string;
  riskLevel?: string;
  assignedRole?: string;
  reviewerId?: string;
  decision?: string;
  rationale?: string;
  createdAt?: string;
  resolvedAt?: string;
};

export type DocumentReviewTasksResponse = {
  tasks: DocumentReviewTask[];
  count: number;
};

export type DocumentFeedbackRequest = {
  userId: string;
  category: string;
  message?: string;
  severity?: string;
};

export type DocumentFeedbackResponse = {
  id?: string;
  documentId?: string;
  tenantId?: string;
  userId?: string;
  category?: string;
  severity?: string;
  message?: string;
  status?: string;
  createdAt?: string;
};

export type DocumentAuditEvent = {
  id?: string;
  tenantId?: string;
  documentId?: string;
  actorId?: string;
  action?: string;
  result?: string;
  reason?: string;
  correlationId?: string;
  timestamp?: string;
};

export type DocumentAuditEventsResponse = {
  events: DocumentAuditEvent[];
  count: number;
};

export type DocumentOperationsMetricsResponse = {
  totalDocuments?: number;
  openReviewTasks?: number;
  quarantinedDocuments?: number;
  feedbackEvents?: number;
  documentsByLifecycleState?: Record<string, number>;
  reviewTasksByStatus?: Record<string, number>;
};

export type KnowledgeIndexRequest = {
  source: string;
  content: string;
};

export type KnowledgeIndexResponse = {
  pointId?: string;
  collection?: string;
  status?: string;
  [key: string]: unknown;
};

export type KnowledgeAnswerRequest = {
  query: string;
  documentId?: string;
  tenantId?: string;
  workspaceId?: string;
  userId?: string;
  roles?: string[];
  authorizedDocumentIds?: string[];
  sensitivityClearance?: string;
};

export type KnowledgeCitation = {
  index?: number;
  chunkId?: string;
  source?: string;
  pageNumber?: number;
  documentType?: string;
  sectionTitle?: string;
  excerpt?: string;
  score?: number;
  [key: string]: unknown;
};

export type CitationPackage = {
  query?: string;
  citations?: KnowledgeCitation[];
  assembledContext?: string;
  [key: string]: unknown;
};

export type KnowledgeAnswerResponse = {
  query?: string;
  answer?: string;
  citationPackage?: CitationPackage;
  status?: string;
  blockedReason?: string | null;
  confidence?: number | null;
  retrievalEventId?: string | null;
  [key: string]: unknown;
};

export type KnowledgeFeedbackRequest = {
  retrievalEventId?: string;
  documentId?: string;
  userId: string;
  category: string;
  message?: string;
  severity?: string;
};

export type KnowledgeFeedbackResponse = {
  id?: string;
  retrievalEventId?: string;
  documentId?: string;
  tenantId?: string;
  userId?: string;
  category?: string;
  severity?: string;
  message?: string;
  status?: string;
  createdAt?: string;
};

export type RetrievalEvent = {
  id?: string;
  tenantId?: string;
  workspaceId?: string;
  userId?: string;
  documentId?: string;
  query?: string;
  status?: string;
  blockedReason?: string;
  citationCount?: number;
  confidence?: number;
  latencyMs?: number;
  createdAt?: string;
};

export type RetrievalEventsResponse = {
  events: RetrievalEvent[];
  count: number;
};

export type KnowledgeEvaluationResponse = {
  totalEvents?: number;
  groundedAnswers?: number;
  blockedAnswers?: number;
  noEvidenceAnswers?: number;
  groundingRate?: number;
  blockRate?: number;
  averageConfidence?: number;
};

export type KnowledgeOperationsMetricsResponse = {
  retrievalEvents?: number;
  feedbackEvents?: number;
  blockedAnswers?: number;
  noEvidenceAnswers?: number;
  eventsByStatus?: Record<string, number>;
};
