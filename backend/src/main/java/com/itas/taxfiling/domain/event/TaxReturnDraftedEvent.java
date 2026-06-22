package com.itas.taxfiling.domain.event;

import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.Getter;

@Getter
public class TaxReturnDraftedEvent extends DomainEvent {
    private final String taxReturnId;
    private final String tin;
    private final TaxTypeCode taxType;
    private final Period period;
    
    public TaxReturnDraftedEvent(String taxReturnId, String tin, TaxTypeCode taxType, Period period) {
        super();
        this.taxReturnId = taxReturnId;
        this.tin = tin;
        this.taxType = taxType;
        this.period = period;
    }
    
    @Override
    public String getAggregateId() {
        return taxReturnId;
    }
    
    @Override
    public String getEventType() {
        return "TaxReturnDrafted";
    }
}
