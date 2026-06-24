package com.itas.taxfiling.application.usecase.officer;

import com.itas.taxfiling.application.port.OfficerReviewItemRepositoryPort;
import com.itas.taxfiling.domain.model.OfficerReviewItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListOfficerReviewQueueUseCase {

    private final OfficerReviewItemRepositoryPort items;

    @Transactional(readOnly = true)
    public List<OfficerReviewItem> execute() {
        return items.findOpenQueue();
    }
}
