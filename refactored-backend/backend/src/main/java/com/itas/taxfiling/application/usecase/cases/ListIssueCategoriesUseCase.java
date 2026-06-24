package com.itas.taxfiling.application.usecase.cases;

import com.itas.taxfiling.application.port.CaseManagementPort;
import com.itas.taxfiling.application.port.CaseManagementPort.IssueCategory;
import com.itas.taxfiling.application.port.CaseManagementPort.SourceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListIssueCategoriesUseCase {

    private final CaseManagementPort caseManagement;

    @Transactional(readOnly = true)
    public List<IssueCategory> execute(String sourceBuc) {
        return caseManagement.listCategoriesFor(new SourceContext("filing", sourceBuc));
    }
}
