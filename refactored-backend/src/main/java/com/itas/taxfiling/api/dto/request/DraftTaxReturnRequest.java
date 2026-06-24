package com.itas.taxfiling.api.dto.request;

import com.itas.taxfiling.domain.valueobject.FilingMethod;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.PeriodFrequency;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DraftTaxReturnRequest(
    @NotBlank String tin,
    @NotBlank String taxType,
    @NotNull LocalDate periodStart,
    @NotNull LocalDate periodEnd,
    @NotNull PeriodFrequency periodFrequency,
    @NotNull FilingMethod method
) {
    public TaxTypeCode toTaxTypeCode() { return new TaxTypeCode(taxType); }
    public Period toPeriod() { return new Period(periodStart, periodEnd, periodFrequency); }
}
