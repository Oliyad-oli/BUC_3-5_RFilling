package com.itas.taxfiling.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Schedule Entity
 * 
 * Represents a schedule within a tax return (e.g., Schedule of Sales, Schedule of Purchases)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {
    private String id;
    private String code;  // e.g., "SCH-A", "SCH-B"
    private String name;  // e.g., "Schedule of Sales"
    private List<LineItem> lineItems;
    
    public static Schedule create(String code, String name) {
        Schedule schedule = new Schedule();
        schedule.id = UUID.randomUUID().toString();
        schedule.code = code;
        schedule.name = name;
        schedule.lineItems = new ArrayList<>();
        return schedule;
    }
    
    public void addLineItem(LineItem lineItem) {
        if (lineItems == null) {
            lineItems = new ArrayList<>();
        }
        lineItems.add(lineItem);
    }
    
    public void removeLineItem(String lineItemId) {
        lineItems.removeIf(item -> item.getId().equals(lineItemId));
    }
    
    public LineItem getLineItem(String lineItemId) {
        return lineItems.stream()
            .filter(item -> item.getId().equals(lineItemId))
            .findFirst()
            .orElse(null);
    }
}
