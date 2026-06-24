package com.itas.taxfiling.api.dto.request;

import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.Money;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record AddLineItemRequest(
    @NotNull UUID entryTypeId,
    @NotNull BigDecimal amount,
    @NotNull String currency,
    @NotNull LineItemSource source,
    Map<String, Object> entryData
) {
    public Money toMoney() { return new Money(amount, currency); }
    public EntrySpecificData toEntryData() {
        return new EntrySpecificData(entryData == null ? Map.of() : entryData);
    }
}
