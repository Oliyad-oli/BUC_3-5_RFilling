package com.itas.taxfiling.application.port;

import java.util.List;

public interface EInvoiceServicePort {
    List<Object> fetchEInvoicesForTaxpayer(String tin, String periodCode);
}
