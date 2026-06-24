package com.itas.taxfiling.application.port;

import com.itas.taxfiling.domain.model.OutboxEntry;
import java.util.List;

public interface OutboxRepositoryPort {
    OutboxEntry save(OutboxEntry entry);
    List<OutboxEntry> findPendingEntries(int limit);
}
