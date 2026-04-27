package me.sathish.runs_app.file_import_record;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

/**
 * Processing statistics collected during the import process.
 */
@Getter
@Setter
public class ProcessingStats {
    public int successCount = 0;
    public int failedCount = 0;
    public int skippedCount = 0;
    public long dbMatchCount = 0;
    public List<FailedRowDetail> failedRowDetails = new ArrayList<>();

    public int getTotalProcessed() {
        return successCount + failedCount + skippedCount;
    }

    public void recordSuccess() {
        this.successCount++;
    }

    public void recordSkip() {
        this.skippedCount++;
    }

    public void recordFailure(String activityId, String errorMessage) {
        this.failedCount++;
        this.failedRowDetails.add(new FailedRowDetail(activityId, errorMessage));
    }

    @Getter
    @Setter
    public static class FailedRowDetail {
        private String activityId;
        private String errorMessage;

        public FailedRowDetail() {}

        public FailedRowDetail(String activityId, String errorMessage) {
            this.activityId = activityId;
            this.errorMessage = errorMessage;
        }
    }
}
