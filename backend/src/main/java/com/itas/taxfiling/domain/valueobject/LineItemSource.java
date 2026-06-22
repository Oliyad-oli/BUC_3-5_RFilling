package com.itas.taxfiling.domain.valueobject;

/**
 * Line Item Source
 * 
 * Indicates the source/origin of a line item value
 */
public enum LineItemSource {
    /**
     * User manually entered the value
     */
    USER_ENTERED,
    
    /**
     * Value computed by calculation engine
     */
    ENGINE_COMPUTED,
    
    /**
     * Value overridden by officer during review
     */
    OFFICER_OVERRIDE;
    
    public boolean isUserEntered() {
        return this == USER_ENTERED;
    }
    
    public boolean isEngineComputed() {
        return this == ENGINE_COMPUTED;
    }
    
    public boolean isOfficerOverride() {
        return this == OFFICER_OVERRIDE;
    }
}
