package me.sathish.runs_app.file_import_record;

import lombok.Getter;
import lombok.Setter;

/**
 * Result of reconciliation verification.
 */
@Getter
@Setter
public class ReconciliationReport {
    private ReconciliationStatus status;
    private String report;
    private String failureReason;
}
