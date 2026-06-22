package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.valueobject.FilingPeriodStatus;
import com.itas.taxfiling.domain.valueobject.Period;
import com.itas.taxfiling.domain.valueobject.TaxTypeCode;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Filing Period Aggregate Root
 * 
 * Represents a filing period for a specific taxpayer and tax type
 */
@Getter
public class FilingPeriod {
    private String id;
    private String tin;
    private TaxTypeCode taxType;
    private Period period;
    private FilingPeriodStatus status;
    private LocalDate dueDate;
    private LocalDate filedDate;
    private String returnId;  // Reference to filed return
    private Instant createdAt;
    private Instant updatedAt;
    
    // Private constructor
    private FilingPeriod() {}
    
    /**
     * Generate a new filing period
     */
    public static FilingPeriod generate(
        String tin,
        TaxTypeCode taxType,
        Period period,
        LocalDate dueDate
    ) {
        FilingPeriod filingPeriod = new FilingPeriod();
        filingPeriod.id = UUID.randomUUID().toString();
        filingPeriod.tin = tin;
        filingPeriod.taxType = taxType;
        filingPeriod.period = period;
        filingPeriod.dueDate = dueDate;
        filingPeriod.status = FilingPeriodStatus.FUTURE;
        filingPeriod.createdAt = Instant.now();
        filingPeriod.updatedAt = Instant.now();
        
        return filingPeriod;
    }
    
    /**
     * Open the period for filing
     */
    public void open() {
        this.status = FilingPeriodStatus.OPEN;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Mark as due (deadline approaching)
     */
    public void markDue() {
        if (status == FilingPeriodStatus.OPEN) {
            this.status = FilingPeriodStatus.DUE;
            this.updatedAt = Instant.now();
        }
    }
    
    /**
     * Mark as overdue
     */
    public void markOverdue() {
        if (status == FilingPeriodStatus.OPEN || status == FilingPeriodStatus.DUE) {
            this.status = FilingPeriodStatus.OVERDUE;
            this.updatedAt = Instant.now();
        }
    }
    
    /**
     * Mark as filed
     */
    public void markFiled(String returnId) {
        if (!status.canFile()) {
            throw new IllegalStateException("Cannot mark as filed in status: " + status);
        }
        
        this.status = FilingPeriodStatus.FILED;
        this.returnId = returnId;
        this.filedDate = LocalDate.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check and update status based on current date
     */
    public void updateStatusBasedOnDate(LocalDate currentDate) {
        if (status == FilingPeriodStatus.FILED) {
            return; // No change if already filed
        }
        
        if (currentDate.isBefore(period.getStartDate())) {
            this.status = FilingPeriodStatus.FUTURE;
        } else if (currentDate.isBefore(dueDate.minusDays(7))) {
            this.status = FilingPeriodStatus.OPEN;
        } else if (currentDate.isBefore(dueDate)) {
            this.status = FilingPeriodStatus.DUE;
        } else {
            this.status = FilingPeriodStatus.OVERDUE;
        }
        
        this.updatedAt = Instant.now();
    }
    
    public boolean isFiled() {
        return status == FilingPeriodStatus.FILED;
    }
    
    public boolean isOverdue() {
        return status == FilingPeriodStatus.OVERDUE;
    }
}
