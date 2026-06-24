package com.itas.taxfiling.api.dto.response;

import java.util.List;

public record OfficerDashboardResponse(
    long totalOpen,
    long totalAssigned,
    long totalDecided,
    long totalClosed,
    List<OfficerReviewItemResponse> items
) {}
