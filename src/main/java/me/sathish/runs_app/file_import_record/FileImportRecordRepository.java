package me.sathish.runs_app.file_import_record;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileImportRecordRepository extends JpaRepository<FileImportRecord, Long> {

    Optional<FileImportRecord> findTopByFileNameOrderByProcessedAtDesc(String fileName);

    Page<FileImportRecord> findByStatus(ProcessingStatus status, Pageable pageable);

    Page<FileImportRecord> findByReconciliationStatus(ReconciliationStatus status, Pageable pageable);

    Page<FileImportRecord> findByStatusAndReconciliationStatus(ProcessingStatus status,
                                                              ReconciliationStatus reconcStatus,
                                                              Pageable pageable);
}
