package com.itas.taxfiling.unit.application.usecase.officer;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.application.port.WorkflowEnginePort;
import com.itas.taxfiling.application.usecase.officer.CreateOfficerReviewItemUseCase;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import com.itas.taxfiling.domain.valueobject.OfficerReviewItemKind;
import com.itas.taxfiling.domain.valueobject.Priority;
import com.itas.taxfiling.domain.valueobject.RiskLevel;
import com.itas.taxfiling.domain.valueobject.RiskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOfficerReviewItemUseCaseTest {

    @Mock OfficerReviewItemRepositoryPort items;
    @Mock WorkflowEnginePort workflow;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks CreateOfficerReviewItemUseCase useCase;

    @Test
    void creates_FRAUD_FLAGGED_item_and_starts_workflow() {
        UUID taxReturnId = UUID.randomUUID();
        when(items.save(any(OfficerReviewItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workflow.startWorkflow(anyString(), any())).thenReturn(UUID.randomUUID());

        OfficerReviewItem item = useCase.execute(taxReturnId,
            new RiskOutcome(RiskLevel.HIGH, "0.95",
                List.of("a", "b", "c", "d", "e"),
                "Risk score 0.95 with 5 anomaly indicators against historical pattern."));

        assertThat(item.getKind()).isEqualTo(OfficerReviewItemKind.FRAUD_FLAGGED);
        assertThat(item.getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(item.getRiskJustification()).contains("Risk score 0.95");
        assertThat(item.getRiskIndicators()).containsExactly("a", "b", "c", "d", "e");
        verify(workflow).startWorkflow(anyString(), any());
    }

    @Test
    void priority_low_when_no_indicators() {
        when(items.save(any(OfficerReviewItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workflow.startWorkflow(anyString(), any())).thenReturn(UUID.randomUUID());

        OfficerReviewItem item = useCase.execute(UUID.randomUUID(),
            new RiskOutcome(RiskLevel.HIGH, "0.50", List.of(),
                "Borderline score, no specific indicators triggered."));
        assertThat(item.getPriority()).isEqualTo(Priority.LOW);
        assertThat(item.getRiskJustification()).contains("Borderline");
    }
}
