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
            @Value("${naitfs.kafka.topics.knowledge-answer-generated}") String knowledgeAnswerGenerated,
            @Value("${naitfs.kafka.topics.payment-audit}") String paymentAudit,
            @Value("${naitfs.kafka.topics.payment-status-changed}") String paymentStatusChanged
    ) {
        return List.of(
                new NewTopic(documentExtracted, 1, (short) 1),
                new NewTopic(knowledgeAnswerGenerated, 1, (short) 1),
                new NewTopic(paymentAudit, 1, (short) 1),
                new NewTopic(paymentStatusChanged, 1, (short) 1)
        );
    }
}
