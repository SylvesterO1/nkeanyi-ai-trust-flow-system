package com.nkeanyi.payment.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingAuditService implements AuditService {

    @Override
    public void record(AuditEvent event) {
        log.info(
                "AUDIT eventType={} actor={} resourceType={} resourceId={} outcome={} metadata={}",
                event.eventType(),
                event.actor(),
                event.resourceType(),
                event.resourceId(),
                event.outcome(),
                event.metadata()
        );
    }
}
