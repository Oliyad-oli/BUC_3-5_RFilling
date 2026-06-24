package com.itas.taxfiling.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * WebClient beans for every engine + sibling-service adapter the refined design wires up.
 * Replace base URLs with actual service endpoints when the real clients land.
 */
@Configuration
public class EngineAdapterConfig {

    /**
     * Real ledger-engine WebClient. Posts go to
     * {@code POST /api/ledger/v1/events/template}. Connect + read timeouts
     * are tight so a slow upstream doesn't pile up requests on our side —
     * Resilience4j's circuit breaker + retry annotations on the adapter
     * methods handle the back-pressure.
     */
    @Bean(name = "ledgerWebClient")
    public WebClient ledgerWebClient(
            @Value("${integrations.ledger.base-url:http://ledger-engine/api}") String baseUrl,
            @Value("${integrations.ledger.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${integrations.ledger.read-timeout-ms:15000}") int readTimeoutMs) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .doOnConnected(conn ->
                conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)));
        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Bean
    public WebClient ruleEngineWebClient(WebClient.Builder builder,
                                          @Value("${integrations.rule-engine.base-url:http://rule-engine/api}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient riskEngineWebClient(WebClient.Builder builder,
                                          @Value("${integrations.risk-engine.base-url:http://risk-engine/api}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient notificationWebClient(WebClient.Builder builder,
                                            @Value("${integrations.notification-engine.base-url:http://notification-engine/api}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient workflowWebClient(WebClient.Builder builder,
                                        @Value("${integrations.workflow-engine.base-url:http://workflow-engine/api}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient taxTypeEngineWebClient(WebClient.Builder builder,
                                             @Value("${integrations.tax-type-engine.base-url:http://tax-type-engine/api}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient dmsWebClient(WebClient.Builder builder,
                                   @Value("${integrations.dms.base-url:http://dms/api}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient caseManagementWebClient(WebClient.Builder builder,
                                              @Value("${integrations.case-management.base-url:http://case-management-service/api}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient eInvoiceWebClient(WebClient.Builder builder,
                                        @Value("${integrations.e-service.base-url:http://e-service/api}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient registrationServiceWebClient(WebClient.Builder builder,
                                                    @Value("${integrations.registration-service.base-url:http://bs-registration-core-server/api}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
