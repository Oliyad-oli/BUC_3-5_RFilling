package com.itas.taxfiling.api.dto.request;

import com.itas.taxfiling.domain.valueobject.EntrySpecificData;
import com.itas.taxfiling.domain.valueobject.Money;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateLineItemRequest(
    BigDecimal amount,
    String currency,
    Map<String, Object> entryData
) {
    public Money toMoneyOrNull() {
        return (amount != null && currency != null) ? new Money(amount, currency) : null;
    }
    public EntrySpecificData toEntryDataOrNull() {
        return entryData == null ? null : new EntrySpecificData(entryData);
    }
}
