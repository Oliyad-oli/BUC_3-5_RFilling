package com.itas.taxfiling.unit.application.usecase.entrytype;

import com.itas.taxfiling.application.port.EventPublisherPort;
import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.application.usecase.entrytype.RegisterLineItemEntryTypeUseCase;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.valueobject.EntryFieldDefinition;
import com.itas.taxfiling.domain.valueobject.EntryFieldType;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterLineItemEntryTypeUseCaseTest {

    @Mock LineItemEntryTypeRepositoryPort entryTypes;
    @Mock EventPublisherPort eventPublisher;
    @InjectMocks RegisterLineItemEntryTypeUseCase useCase;

    @Test
    void inserts_with_next_version() {
        when(entryTypes.nextVersionForCode(anyString())).thenReturn(3);
        when(entryTypes.save(any(LineItemEntryType.class))).thenAnswer(inv -> inv.getArgument(0));

        LineItemEntryType saved = useCase.execute("VAT_SALES",
            new TaxTypeCode("VAT"), ScheduleKind.SALES,
            List.of(new EntryFieldDefinition("k", "K", EntryFieldType.STRING, true,
                List.of(), null)),
            "admin-1");

        assertThat(saved.getVersion()).isEqualTo(3);
    }
}
