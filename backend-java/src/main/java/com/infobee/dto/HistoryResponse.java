package com.infobee.dto;

import com.infobee.model.RequestHistory;
import com.infobee.model.RequestStatus;
import java.time.Instant;

public record HistoryResponse(Long id, String requestType, Long requestId, Long actorId, String actorUsername,
                              RequestStatus fromStatus, RequestStatus toStatus, String note, Instant createdAt) {
    public static HistoryResponse from(RequestHistory h) {
        return new HistoryResponse(h.getId(), h.getRequestType().name(), h.getRequestId(), h.getActor().getId(),
            h.getActor().getUsername(), h.getFromStatus(), h.getToStatus(), h.getNote(), h.getCreatedAt());
    }
}
