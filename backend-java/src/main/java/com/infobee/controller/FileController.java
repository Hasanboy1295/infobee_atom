package com.infobee.controller;

import com.infobee.dto.AttachmentResponse;
import com.infobee.model.Attachment;
import com.infobee.repository.AttachmentRepository;
import com.infobee.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Upload", description = "File upload and download for ATOM/CPSR requests")
@SecurityRequirement(name = "bearerAuth")
public class FileController {
    private final FileStorageService fileStorageService;
    private final AttachmentRepository attachmentRepository;

    public FileController(FileStorageService fileStorageService, AttachmentRepository attachmentRepository) {
        this.fileStorageService = fileStorageService;
        this.attachmentRepository = attachmentRepository;
    }

    @PostMapping("/{type}/{requestId}")
    @Operation(summary = "Upload file", description = "Upload a file to an ATOM or CPSR request. Type must be ATOM or CPSR.")
    public AttachmentResponse upload(
        @PathVariable String type,
        @PathVariable Long requestId,
        @RequestParam("file") MultipartFile file,
        Authentication auth
    ) {
        Attachment attachment = fileStorageService.upload(type.toUpperCase(), requestId, file, auth.getName());
        return AttachmentResponse.from(attachment);
    }

    @GetMapping("/{type}/{requestId}")
    @Operation(summary = "List attachments", description = "List all files attached to an ATOM or CPSR request.")
    public List<AttachmentResponse> list(
        @PathVariable String type,
        @PathVariable Long requestId,
        Authentication auth
    ) {
        List<Attachment> attachments;
        if ("ATOM".equalsIgnoreCase(type)) {
            attachments = attachmentRepository.findByAtomRequestIdOrderByCreatedAtAsc(requestId);
        } else {
            attachments = attachmentRepository.findByCpsrRequestIdOrderByCreatedAtAsc(requestId);
        }
        return attachments.stream().map(AttachmentResponse::from).toList();
    }

    @GetMapping("/download/{storedFilename}")
    @Operation(summary = "Download file", description = "Download a previously uploaded file by its stored filename.")
    public ResponseEntity<Resource> download(@PathVariable String storedFilename,
                                             Authentication auth) throws java.net.MalformedURLException {
        Attachment attachment = attachmentRepository.findByStoredFilename(storedFilename)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            boolean isOwner = attachment.getUploadedBy() != null
                && attachment.getUploadedBy().getUsername().equals(auth.getName());
            if (!isOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        var filePath = fileStorageService.resolve(storedFilename);
        Resource resource = new UrlResource(filePath.toUri());
        String encoded = URLEncoder.encode(storedFilename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
            .body(resource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete attachment", description = "Delete a file attachment by its ID.")
    public void delete(@PathVariable Long id, Authentication auth) {
        Attachment attachment = attachmentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            boolean isOwner = attachment.getUploadedBy() != null
                && attachment.getUploadedBy().getUsername().equals(auth.getName());
            if (!isOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        attachmentRepository.deleteById(id);
    }
}
