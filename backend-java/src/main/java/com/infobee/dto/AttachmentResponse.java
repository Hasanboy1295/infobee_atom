package com.infobee.dto;

import com.infobee.model.Attachment;
import java.time.Instant;

public record AttachmentResponse(
    Long id,
    String originalFilename,
    String storedFilename,
    String contentType,
    Long size,
    String checksum,
    Long uploadedById,
    String uploadedByUsername,
    Instant createdAt
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(
            a.getId(),
            a.getOriginalFilename(),
            a.getStoredFilename(),
            a.getContentType(),
            a.getSize(),
            a.getChecksum(),
            a.getUploadedBy().getId(),
            a.getUploadedBy().getUsername(),
            a.getCreatedAt()
        );
    }
}
