package com.nkeanyi.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class GatewayRouteConfigTest {

    @Test
    void shouldExposeAllRegulatedWorkflowRoutes() {
        Properties properties = applicationProperties();

        assertThat(properties)
                .containsValue("document-intelligence-service")
                .containsValue("${DOCUMENT_INTELLIGENCE_SERVICE_URL:http://localhost:8089}")
                .containsValue("Path=/api/v1/documents/**")
                .containsValue("knowledge-service")
                .containsValue("${KNOWLEDGE_SERVICE_URL:http://localhost:8088}")
                .containsValue("Path=/api/v1/knowledge/**")
                .containsValue("payment-orchestration-service")
                .containsValue("${PAYMENT_ORCHESTRATION_SERVICE_URL:http://localhost:8082}")
                .containsValue("Path=/api/v1/payments/**")
                .containsValue("compliance-evidence-service")
                .containsValue("${COMPLIANCE_EVIDENCE_SERVICE_URL:http://localhost:8084}")
                .containsValue("Path=/api/v1/evidence/**");
    }

    private Properties applicationProperties() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        yaml.afterPropertiesSet();
        return yaml.getObject();
    }
}
