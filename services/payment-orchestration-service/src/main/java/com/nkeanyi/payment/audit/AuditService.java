package com.nkeanyi.payment.audit;

public interface AuditService {
    void record(AuditEvent event);
}
