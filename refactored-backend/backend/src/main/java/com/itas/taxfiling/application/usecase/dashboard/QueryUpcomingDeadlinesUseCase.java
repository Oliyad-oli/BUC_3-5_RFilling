package com.itas.taxfiling.application.usecase.dashboard;

import com.itas.taxfiling.application.port.TaxTypeEnginePort;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.TaxTypeSummary;
import com.itas.taxfiling.application.port.TaxTypeEnginePort.UpcomingPeriod;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * BUC-FIL-045 — internal API. Returns upcoming filing deadlines per tax type
 * for the next N days. Phase 2 callers: scheduled-tasks uses this to drive
 * reminder dispatch via notification-engine.
 */
@Service
@RequiredArgsConstructor
public class QueryUpcomingDeadlinesUseCase {

    private final TaxTypeEnginePort taxTypeEngine;

    @Transactional(readOnly = true)
    public List<UpcomingPeriod> execute(LocalDate from) {
        return taxTypeEngine.listAvailableTaxTypes().stream()
            .filter(TaxTypeSummary::active)
            .map(t -> taxTypeEngine.nextPeriod(new TaxTypeCode(t.code()), from))
            .toList();
    }
}
