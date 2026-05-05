package me.sathish.runs_app.file_import_record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import me.sathish.runs_app.run_app_user.RunAppUser;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class FileImportRecord {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(
            name = "file_import_record_sequence",
            sequenceName = "file_import_record_sequence",
            allocationSize = 1,
            initialValue = 10000
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "file_import_record_sequence"
    )
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String fileName;

    @Column(nullable = false)
    private Integer expectedRowCount;

    @Column(nullable = false)
    private Integer successCount = 0;

    @Column(nullable = false)
    private Integer failedCount = 0;

    @Column(nullable = false)
    private Integer skippedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String failureDetails;  // JSON array of failed rows with errors

    @Column(columnDefinition = "TEXT")
    private String reconciliationReport;  // Detailed reconciliation report

    @Column(nullable = false)
    private Integer retryCount = 0;  // Number of retry attempts

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;  // Timestamp of last retry attempt

    @Column(name = "email_alert_sent_at")
    private LocalDateTime emailAlertSentAt;  // Timestamp when failure alert was sent
    @JsonIgnore
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime processedAt;
    @JsonIgnore
    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private RunAppUser createdBy;

}
