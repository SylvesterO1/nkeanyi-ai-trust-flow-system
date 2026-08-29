package com.nkeanyi.compliance.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public List<NewTopic> complianceInputTopics(
            @Value("${naitfs.kafka.topics.document-extracted}") String documentExtracted,
            @Value("${naitfs.kafka.topics.document-extraction-failed}") String documentExtractionFailed,
            @Value("${naitfs.kafka.topics.knowledge-answer-generated}") String knowledgeAnswerGenerated,
            @Value("${naitfs.kafka.topics.knowledge-answer-failed}") String knowledgeAnswerFailed,
            @Value("${naitfs.kafka.topics.payment-received}") String paymentReceived,
            @Value("${naitfs.kafka.topics.payment-audit}") String paymentAudit,
            @Value("${naitfs.kafka.topics.payment-status-changed}") String paymentStatusChanged,
            @Value("${naitfs.kafka.topics.payment-processed}") String paymentProcessed,
            @Value("${naitfs.kafka.topics.payment-rejected}") String paymentRejected
    ) {
        return List.of(
                new NewTopic(documentExtracted, 1, (short) 1),
                new NewTopic(documentExtractionFailed, 1, (short) 1),
                new NewTopic(knowledgeAnswerGenerated, 1, (short) 1),
                new NewTopic(knowledgeAnswerFailed, 1, (short) 1),
                new NewTopic(paymentReceived, 1, (short) 1),
                new NewTopic(paymentAudit, 1, (short) 1),
                new NewTopic(paymentStatusChanged, 1, (short) 1),
                new NewTopic(paymentProcessed, 1, (short) 1),
                new NewTopic(paymentRejected, 1, (short) 1)
        );
    }
}
