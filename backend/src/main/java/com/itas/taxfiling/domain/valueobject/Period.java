package com.itas.taxfiling.domain.valueobject;

import lombok.Value;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Period Value Object
 * 
 * Represents a time period with start and end dates
 */
@Value
public class Period {
    LocalDate startDate;
    LocalDate endDate;
    
    public Period(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
        
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    public static Period of(LocalDate startDate, LocalDate endDate) {
        return new Period(startDate, endDate);
    }
    
    public static Period month(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return new Period(start, end);
    }
    
    public static Period day(LocalDate date) {
        return new Period(date, date);
    }
    
    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }
    
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    public boolean overlaps(Period other) {
        return !this.endDate.isBefore(other.startDate) && 
               !other.endDate.isBefore(this.startDate);
    }
    
    public boolean isDaily() {
        return startDate.equals(endDate);
    }
    
    public boolean isMonthly() {
        return startDate.getDayOfMonth() == 1 && 
               endDate.equals(startDate.plusMonths(1).minusDays(1));
    }
    
    @Override
    public String toString() {
        if (isDaily()) {
            return startDate.toString();
        }
        return String.format("%s to %s", startDate, endDate);
    }
}
