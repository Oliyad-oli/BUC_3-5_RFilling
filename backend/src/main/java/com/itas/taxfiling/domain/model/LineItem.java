package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.valueobject.LineItemSource;
import com.itas.taxfiling.domain.valueobject.Money;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Line Item Entity
 * 
 * Represents a line item within a schedule (e.g., a sales transaction, a purchase)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineItem {
    private String id;
    private String lineCode;  // e.g., "LINE-001"
    private String description;
    private Money amount;
    private LineItemSource source;
    private String referenceId;  // Reference to e-invoice or external document
    
    public static LineItem create(String lineCode, String description, Money amount, LineItemSource source) {
        LineItem item = new LineItem();
        item.id = UUID.randomUUID().toString();
        item.lineCode = lineCode;
        item.description = description;
        item.amount = amount;
        item.source = source;
        return item;
    }
    
    public static LineItem fromUserInput(String lineCode, String description, Money amount) {
        return create(lineCode, description, amount, LineItemSource.USER_ENTERED);
    }
    
    public static LineItem fromEInvoice(String lineCode, String description, Money amount, String einvoiceRef) {
        LineItem item = create(lineCode, description, amount, LineItemSource.USER_ENTERED);
        item.referenceId = einvoiceRef;
        return item;
    }
    
    public static LineItem computed(String lineCode, String description, Money amount) {
        return create(lineCode, description, amount, LineItemSource.ENGINE_COMPUTED);
    }
    
    public void updateAmount(Money newAmount) {
        this.amount = newAmount;
    }
    
    public boolean isUserEntered() {
        return source == LineItemSource.USER_ENTERED;
    }
    
    public boolean isComputed() {
        return source == LineItemSource.ENGINE_COMPUTED;
    }
}
