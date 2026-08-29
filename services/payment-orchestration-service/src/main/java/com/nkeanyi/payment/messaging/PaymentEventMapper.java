package com.nkeanyi.payment.messaging;

import com.nkeanyi.payment.dto.CreatePaymentRequest;
import com.nkeanyi.payment.entity.Payment;
import com.nkeanyi.payment.event.PaymentEvent;
import com.nkeanyi.payment.observability.MdcContextReader;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventMapper {

    public PaymentEvent from(Payment payment, CreatePaymentRequest request) {
        PaymentEvent event = new PaymentEvent();
        event.setPaymentId(payment.getPaymentId());
        event.setTenantId(MdcContextReader.tenantId());
        event.setCorrelationId(MdcContextReader.correlationId());
        event.setIdempotencyKey(payment.getIdempotencyKey());
        event.setCustomerId(payment.getCustomerId());
        event.setSourceAccount(request.getSourceAccount());
        event.setDestinationAccount(request.getDestinationAccount());
        event.setAmount(request.getAmount());
        event.setCurrency(request.getCurrency());
        event.setPaymentReference(request.getPaymentReference());
        event.setPaymentMethod(request.getPaymentMethod());
        event.setNarration(request.getNarration());
        return event;
    }
}
