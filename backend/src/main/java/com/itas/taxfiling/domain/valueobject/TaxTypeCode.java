package com.itas.taxfiling.domain.valueobject;

/**
 * Tax Type Code
 * 
 * Represents the type of tax being filed
 */
public enum TaxTypeCode {
    VAT,           // Value Added Tax
    INCOME_TAX,    // Corporate Income Tax
    WHT,           // Withholding Tax
    PAYE,          // Pay As You Earn
    EXCISE;        // Excise Duty
    
    public boolean isVAT() {
        return this == VAT;
    }
    
    public boolean isIncomeTax() {
        return this == INCOME_TAX;
    }
}
