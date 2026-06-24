package com.itas.taxfiling.application.usecase.entrytype;

import com.itas.taxfiling.application.port.LineItemEntryTypeRepositoryPort;
import com.itas.taxfiling.domain.model.LineItemEntryType;
import com.itas.taxfiling.domain.valueobject.EntryTypeStatus;
import com.itas.taxfiling.domain.valueobject.ScheduleKind;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListLineItemEntryTypesUseCase {

    private final LineItemEntryTypeRepositoryPort entryTypes;

    @Transactional(readOnly = true)
    public List<LineItemEntryType> execute(TaxTypeCode taxType, ScheduleKind kind) {
        return entryTypes.findByTaxTypeAndKindAndStatus(taxType, kind, EntryTypeStatus.ACTIVE);
    }
}
