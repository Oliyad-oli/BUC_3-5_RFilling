package com.itas.taxfiling.domain.valueobject;

import java.util.Objects;

/** Versioned rule package reference from the tax-type engine. */
public record RulePackageVersion(String taxTypeCode, String version, String checksum) {

    public RulePackageVersion {
        Objects.requireNonNull(taxTypeCode, "taxTypeCode");
        Objects.requireNonNull(version, "version");
    }
}
