package com.infobee.service;

import com.infobee.model.ActivityLog;
import com.infobee.model.Attachment;
import com.infobee.model.AtomRequest;
import com.infobee.model.CpsrRequest;
import com.infobee.model.RequestType;
import com.infobee.model.User;
import com.infobee.repository.AttachmentRepository;
import com.infobee.repository.AtomRequestRepository;
import com.infobee.repository.CpsrRequestRepository;
import com.infobee.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileStorageService {
    private final Path uploadDir;
    private final AttachmentRepository attachmentRepository;
    private final AtomRequestRepository atomRepository;
    private final CpsrRequestRepository cpsrRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    public FileStorageService(
        @Value("${app.upload.dir:uploads}") String uploadDir,
        AttachmentRepository attachmentRepository,
        AtomRequestRepository atomRepository,
        CpsrRequestRepository cpsrRepository,
        UserRepository userRepository,
        ActivityLogService activityLogService
    ) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
        this.attachmentRepository = attachmentRepository;
        this.atomRepository = atomRepository;
        this.cpsrRepository = cpsrRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public Attachment upload(String type, Long requestId, MultipartFile file, String username) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String storedFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path targetPath = uploadDir.resolve(storedFilename);

        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        String checksum;
        try {
            checksum = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(targetPath)));
        } catch (NoSuchAlgorithmException | IOException e) {
            checksum = "unknown";
        }

        Attachment attachment = new Attachment();
        attachment.setOriginalFilename(file.getOriginalFilename());
        attachment.setStoredFilename(storedFilename);
        attachment.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.setSize(file.getSize());
        attachment.setChecksum(checksum);
        attachment.setUploadedBy(user);

        if ("ATOM".equalsIgnoreCase(type)) {
            AtomRequest request = atomRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ATOM request not found"));
            attachment.setAtomRequest(request);
        } else if ("CPSR".equalsIgnoreCase(type)) {
            CpsrRequest request = cpsrRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CPSR request not found"));
            attachment.setCpsrRequest(request);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request type");
        }

        Attachment saved = attachmentRepository.save(attachment);
        RequestType reqType = "ATOM".equalsIgnoreCase(type) ? RequestType.ATOM : RequestType.CPSR;
        activityLogService.log(user, ActivityLog.Action.FILE_UPLOADED, reqType, requestId,
            file.getOriginalFilename() + " (" + file.getSize() + " bytes)", null);
        return saved;
    }

    public Path resolve(String storedFilename) {
        Path filePath = uploadDir.resolve(storedFilename).normalize();
        if (!filePath.startsWith(uploadDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }
        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return filePath;
    }
}
