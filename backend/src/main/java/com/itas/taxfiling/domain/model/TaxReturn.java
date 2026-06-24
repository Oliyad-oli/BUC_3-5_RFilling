package com.itas.taxfiling.domain.model;

import com.itas.taxfiling.domain.event.DomainEvent;
import com.itas.taxfiling.domain.exception.InvalidStateTransitionException;
import com.itas.taxfiling.domain.valueobject.*;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TaxReturn Aggregate Root
 * 
 * Core aggregate for BUC-003 (Return Filing)
 * Represents a tax return filing for a specific period
 */
@Getter
public class TaxReturn {
    private String id;
    private String tin;
    private TaxTypeCode taxType;
    private Period period;
    private TaxReturnStatus status;
    private List<Schedule> schedules;
    private List<CalculationIteration> iterations;
    private List<Amendment> amendments;
    private String currentIterationId;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    
    // Domain events
    private List<DomainEvent> domainEvents = new ArrayList<>();
    
    // Private constructor for reconstitution
    private TaxReturn() {
        this.schedules = new ArrayList<>();
        this.iterations = new ArrayList<>();
        this.amendments = new ArrayList<>();
    }
    
    /**
     * Draft a new tax return
     */
    public static TaxReturn draft(String tin, TaxTypeCode taxType, Period period, String createdBy) {
        TaxReturn taxReturn = new TaxReturn();
        taxReturn.id = UUID.randomUUID().toString();
        taxReturn.tin = tin;
        taxReturn.taxType = taxType;
        taxReturn.period = period;
        taxReturn.status = TaxReturnStatus.DRAFT;
        taxReturn.createdAt = Instant.now();
        taxReturn.updatedAt = Instant.now();
        taxReturn.createdBy = createdBy;
        
        // Add domain event
        // taxReturn.addEvent(new TaxReturnDraftedEvent(taxReturn.id, tin, taxType, period));
        
        return taxReturn;
    }
    
    /**
     * Add a schedule to the return
     */
    public void addSchedule(Schedule schedule) {
        if (!status.isDraft()) {
            throw new InvalidStateTransitionException(status.name(), "add schedule");
        }
        
        this.schedules.add(schedule);
        this.updatedAt = Instant.now();
        
        // addEvent(new ScheduleAddedEvent(this.id, schedule.getId()));
    }
    
    /**
     * Request calculation
     */
    public void requestCalculation() {
        if (!status.isDraft() && !status.equals(TaxReturnStatus.AMENDMENT_DRAFT)) {
            throw new InvalidStateTransitionException(status.name(), "request calculation");
        }
        
        if (status.isDraft()) {
            this.status = TaxReturnStatus.CALCULATING;
        } else {
            this.status = TaxReturnStatus.AMENDMENT_CALCULATING;
        }
        
        this.updatedAt = Instant.now();
        
        // addEvent(new CalculationRequestedEvent(this.id));
    }
    
    /**
     * Record calculation result
     */
    public void recordCalculation(CalculationIteration iteration) {
        if (!status.isCalculating()) {
            throw new InvalidStateTransitionException(status.name(), "record calculation");
        }
        
        this.iterations.add(iteration);
        this.currentIterationId = iteration.getId();
        this.status = TaxReturnStatus.DRAFT; // Back to draft for review
        this.updatedAt = Instant.now();
        
        // addEvent(new CalculationCompletedEvent(this.id, iteration.getId()));
    }
    
    /**
     * Accept calculation (submit return)
     */
    public void acceptCalculation(String iterationId) {
        if (!status.isDraft() && !status.equals(TaxReturnStatus.AMENDMENT_DRAFT)) {
            throw new InvalidStateTransitionException(status.name(), "accept calculation");
        }
        
        if (!iterationId.equals(currentIterationId)) {
            throw new IllegalArgumentException("Can only accept the current iteration");
        }
        
        if (status.isDraft()) {
            this.status = TaxReturnStatus.ACCEPTED;
        } else {
            this.status = TaxReturnStatus.AMENDMENT_ACCEPTED;
        }
        
        this.updatedAt = Instant.now();
        
        // addEvent(new CalculationAcceptedEvent(this.id, iterationId));
    }
    
    /**
     * Mark as posted to ledger
     */
    public void markPostedToLedger() {
        if (!status.isAccepted()) {
            throw new InvalidStateTransitionException(status.name(), "post to ledger");
        }
        
        if (status == TaxReturnStatus.ACCEPTED) {
            this.status = TaxReturnStatus.POSTED_TO_LEDGER;
        } else {
            this.status = TaxReturnStatus.AMENDMENT_POSTED;
        }
        
        this.updatedAt = Instant.now();
        
        // addEvent(new PostedToLedgerEvent(this.id));
    }
    
    /**
     * Start post-ledger validation
     */
    public void startPostLedgerValidation() {
        if (!status.isPostedToLedger()) {
            throw new InvalidStateTransitionException(status.name(), "start validation");
        }
        
        this.status = TaxReturnStatus.UNDER_VALIDATION;
        this.updatedAt = Instant.now();
        
        // addEvent(new PostLedgerValidationStartedEvent(this.id));
    }
    
    /**
     * Mark as completed (validation passed)
     */
    public void markCompleted() {
        if (status != TaxReturnStatus.UNDER_VALIDATION) {
            throw new InvalidStateTransitionException(status.name(), "mark completed");
        }
        
        this.status = TaxReturnStatus.COMPLETED;
        this.updatedAt = Instant.now();
        
        // addEvent(new TaxReturnCompletedEvent(this.id));
    }
    
    /**
     * Flag for manual review
     */
    public void flagForReview() {
        if (status != TaxReturnStatus.UNDER_VALIDATION) {
            throw new InvalidStateTransitionException(status.name(), "flag for review");
        }
        
        this.status = TaxReturnStatus.MANUAL_REVIEW;
        this.updatedAt = Instant.now();
        
        // addEvent(new TaxReturnFraudFlaggedEvent(this.id));
    }
    
    /**
     * Confirm fraud
     */
    public void confirmFraud() {
        if (status != TaxReturnStatus.MANUAL_REVIEW) {
            throw new InvalidStateTransitionException(status.name(), "confirm fraud");
        }
        
        this.status = TaxReturnStatus.FRAUD_CONFIRMED;
        this.updatedAt = Instant.now();
        
        // addEvent(new FraudConfirmedEvent(this.id));
    }
    
    /**
     * Request amendment
     */
    public void requestAmendment(String reason) {
        if (!status.canBeAmended()) {
            throw new InvalidStateTransitionException(status.name(), "request amendment");
        }
        
        this.status = TaxReturnStatus.AMENDMENT_DRAFT;
        this.updatedAt = Instant.now();
        
        // Amendment amendment = new Amendment(UUID.randomUUID().toString(), reason);
        // this.amendments.add(amendment);
        
        // addEvent(new AmendmentRequestedEvent(this.id, reason));
    }
    
    /**
     * Clear by officer (from manual review)
     */
    public void clearByOfficer() {
        if (status != TaxReturnStatus.MANUAL_REVIEW) {
            throw new InvalidStateTransitionException(status.name(), "clear");
        }
        
        this.status = TaxReturnStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }
    
    // Helper methods
    
    public boolean canAddSchedules() {
        return status.isDraft();
    }
    
    public boolean canCalculate() {
        return status.isDraft() || status == TaxReturnStatus.AMENDMENT_DRAFT;
    }
    
    public boolean canSubmit() {
        return status.isDraft() && currentIterationId != null;
    }
    
    public Money getTotalTax() {
        // Calculate from iterations
        if (currentIterationId == null) {
            return Money.zero("ETB");
        }
        
        return iterations.stream()
            .filter(it -> it.getId().equals(currentIterationId))
            .findFirst()
            .map(CalculationIteration::getNetTax)
            .orElse(Money.zero("ETB"));
    }
    
    // Event management
    
    private void addEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }
    
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
