package com.nkeanyi.payment.config;

import com.nkeanyi.payment.messaging.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public List<NewTopic> paymentTopics() {
        return List.of(
                new NewTopic(KafkaTopics.PAYMENT_RECEIVED, 1, (short) 1),
                new NewTopic(KafkaTopics.FRAUD_ASSESSED, 1, (short) 1),
                new NewTopic(KafkaTopics.COMPLIANCE_ASSESSED, 1, (short) 1),
                new NewTopic(KafkaTopics.PAYMENT_PROCESSED, 1, (short) 1),
                new NewTopic(KafkaTopics.PAYMENT_REJECTED, 1, (short) 1),
                new NewTopic(KafkaTopics.PAYMENT_FAILED, 1, (short) 1),
                new NewTopic(KafkaTopics.PAYMENT_RECEIVED_DLT, 1, (short) 1)
        );
    }
}
