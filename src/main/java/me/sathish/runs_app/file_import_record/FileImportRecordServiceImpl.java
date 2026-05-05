package me.sathish.runs_app.file_import_record;

import lombok.extern.slf4j.Slf4j;
import me.sathish.runs_app.run_app_user.RunAppUser;
import me.sathish.runs_app.run_app_user.RunAppUserRepository;
import me.sathish.runs_app.security.RunsAppSecurityUserDetails;
import me.sathish.runs_app.util.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class FileImportRecordServiceImpl implements FileImportRecordService {

    private final FileImportRecordRepository repository;
    private final RunAppUserRepository runAppUserRepository;

    public FileImportRecordServiceImpl(FileImportRecordRepository repository,
                                       RunAppUserRepository runAppUserRepository) {
        this.repository = repository;
        this.runAppUserRepository = runAppUserRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FileImportRecord createImportRecord(String fileName, int expectedRowCount) {
        FileImportRecord record = new FileImportRecord();
        record.setFileName(fileName);
        record.setExpectedRowCount(expectedRowCount);
        record.setStatus(ProcessingStatus.PROCESSING);
        record.setReconciliationStatus(ReconciliationStatus.PENDING);
        record.setSuccessCount(0);
        record.setFailedCount(0);
        record.setSkippedCount(0);
        resolveCurrentUser().ifPresent(record::setCreatedBy);

        FileImportRecord saved = repository.save(record);
        log.info("Created import record for: {} with {} expected rows", fileName, expectedRowCount);
        return saved;
    }

    private Optional<RunAppUser> resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof RunsAppSecurityUserDetails userDetails) {
            return runAppUserRepository.findById(userDetails.getId());
        }

        return Optional.empty();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsCompleted(String fileName, int success, int failed, int skipped,
                               ProcessingStatus status, ReconciliationStatus reconcStatus) {
        markAsCompleted(fileName, success, failed, skipped, status, reconcStatus, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsCompleted(String fileName, int success, int failed, int skipped,
                               ProcessingStatus status, ReconciliationStatus reconcStatus,
                               String reconcReport) {
        FileImportRecord record = repository.findTopByFileNameOrderByProcessedAtDesc(fileName)
            .orElseThrow(() -> new NotFoundException("Import record not found: " + fileName));

        record.setSuccessCount(success);
        record.setFailedCount(failed);
        record.setSkippedCount(skipped);
        record.setStatus(status);
        record.setReconciliationStatus(reconcStatus);
        record.setReconciliationReport(reconcReport);

        repository.save(record);
        log.info("Import record completed: {} | Status: {} | Reconciliation: {} | (Success: {}, Failed: {}, Skipped: {})",
            fileName, status, reconcStatus, success, failed, skipped);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(String fileName, ProcessingStatus status,
                            ReconciliationStatus reconcStatus, String reason) {
        FileImportRecord record = repository.findTopByFileNameOrderByProcessedAtDesc(fileName)
            .orElseThrow(() -> new NotFoundException("Import record not found: " + fileName));

        record.setStatus(status);
        record.setReconciliationStatus(reconcStatus);
        record.setReconciliationReport(reason);

        repository.save(record);
        log.error("Import failed: {} | Status: {} | Reason: {}", fileName, status, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public FileImportRecord getByFileName(String fileName) {
        return repository.findTopByFileNameOrderByProcessedAtDesc(fileName)
            .orElseThrow(() -> new NotFoundException("Import record not found: " + fileName));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileImportRecord> findByStatus(ProcessingStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileImportRecord> findByReconciliationStatus(ReconciliationStatus status, Pageable pageable) {
        return repository.findByReconciliationStatus(status, pageable);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementRetryCount(String fileName) {
        FileImportRecord record = repository.findTopByFileNameOrderByProcessedAtDesc(fileName)
            .orElseThrow(() -> new NotFoundException("Import record not found: " + fileName));

        record.setRetryCount(record.getRetryCount() + 1);
        record.setLastRetryAt(LocalDateTime.now());
        record.setStatus(ProcessingStatus.RETRY_IN_PROGRESS);

        repository.save(record);
        log.info("Incremented retry count for: {} (Retry attempt: {})", fileName, record.getRetryCount());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendEmailAlert(FileImportRecord record, int maxRetryAttempts) {
        // Send alert if: retry count >= max attempts AND there are failed rows AND alert not yet sent
        return record.getRetryCount() >= maxRetryAttempts
            && record.getFailedCount() > 0
            && record.getEmailAlertSentAt() == null;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEmailAlertSent(String fileName) {
        FileImportRecord record = repository.findTopByFileNameOrderByProcessedAtDesc(fileName)
            .orElseThrow(() -> new NotFoundException("Import record not found: " + fileName));

        record.setEmailAlertSentAt(LocalDateTime.now());
        repository.save(record);
        log.info("Marked email alert as sent for: {}", fileName);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateFailureDetails(String fileName, String failureDetailsJson) {
        FileImportRecord record = repository.findTopByFileNameOrderByProcessedAtDesc(fileName)
            .orElseThrow(() -> new NotFoundException("Import record not found: " + fileName));

        record.setFailureDetails(failureDetailsJson);
        repository.save(record);
        log.debug("Updated failure details for: {}", fileName);
    }
}
