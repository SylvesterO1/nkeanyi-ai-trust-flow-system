package com.nkeanyi.payment.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class PaymentTracingService {

    private final Tracer tracer;

    public PaymentTracingService(Tracer tracer) {
        this.tracer = tracer;
    }

    public <T> T inSpan(String spanName, Supplier<T> action) {
        Span span = tracer.nextSpan().name(spanName).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            return action.get();
        } catch (RuntimeException ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    public void annotateCurrentSpan(String key, String value) {
        Span current = tracer.currentSpan();
        if (current != null) {
            current.tag(key, value);
        }
    }
}
