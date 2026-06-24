package com.itas.taxfiling.domain.valueobject;

/** Schedule kind — matches the polymorphic line-item model (Rule 5). */
public enum ScheduleKind {
    SALES,
    PURCHASES,
    WITHHOLDING,
    OTHER
}
