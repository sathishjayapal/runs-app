package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
@Slf4j
public class UnifiedGarminImportService {

    private final GarminFitImportService fitImportService;
    private final GarminCsvImportService csvImportService;
    private final ExecutorService executorService;

    public UnifiedGarminImportService(GarminFitImportService fitImportService,
                                       GarminCsvImportService csvImportService) {
        this.fitImportService = fitImportService;
        this.csvImportService = csvImportService;
        this.executorService = Executors.newFixedThreadPool(2);
    }

    public UnifiedImportResult processAllFiles() {
        return processAllFiles(null);
    }

    public UnifiedImportResult processAllFiles(String csvFolderOverride) {
        log.info("=== Starting Unified Garmin Import (FIT + CSV in parallel) ===");
        
        UnifiedImportResult unifiedResult = new UnifiedImportResult();
        long startTime = System.currentTimeMillis();

        Future<ImportResult> fitFuture = executorService.submit(() -> {
            log.info("Thread [FIT] - Starting FIT file processing");
            try {
                ImportResult result = fitImportService.processImportFolder();
                log.info("Thread [FIT] - Completed: Success={}, Skipped={}, Failed={}", 
                    result.getSuccessCount(), result.getSkippedCount(), result.getFailedCount());
                return result;
            } catch (Exception e) {
                log.error("Thread [FIT] - Error during processing", e);
                throw e;
            }
        });

        Future<ImportResult> csvFuture = executorService.submit(() -> {
            log.info("Thread [CSV] - Starting CSV file processing");
            try {
                ImportResult result = csvImportService.processImportFolder(csvFolderOverride);
                log.info("Thread [CSV] - Completed: Success={}, Skipped={}, Failed={}", 
                    result.getSuccessCount(), result.getSkippedCount(), result.getFailedCount());
                return result;
            } catch (Exception e) {
                log.error("Thread [CSV] - Error during processing", e);
                throw e;
            }
        });

        try {
            ImportResult fitResult = fitFuture.get(5, TimeUnit.MINUTES);
            unifiedResult.setFitResult(fitResult);
            log.info("FIT import completed successfully");
        } catch (TimeoutException e) {
            log.error("FIT import timed out after 5 minutes");
            fitFuture.cancel(true);
            unifiedResult.setFitError("Timeout after 5 minutes");
        } catch (Exception e) {
            log.error("FIT import failed with exception", e);
            unifiedResult.setFitError(e.getMessage());
        }

        try {
            ImportResult csvResult = csvFuture.get(5, TimeUnit.MINUTES);
            unifiedResult.setCsvResult(csvResult);
            log.info("CSV import completed successfully");
        } catch (TimeoutException e) {
            log.error("CSV import timed out after 5 minutes");
            csvFuture.cancel(true);
            unifiedResult.setCsvError("Timeout after 5 minutes");
        } catch (Exception e) {
            log.error("CSV import failed with exception", e);
            unifiedResult.setCsvError(e.getMessage());
        }

        long duration = System.currentTimeMillis() - startTime;
        unifiedResult.setTotalDurationMs(duration);

        logReconciliation(unifiedResult);
        
        log.info("=== Unified Garmin Import Completed in {}ms ===", duration);
        return unifiedResult;
    }

    private void logReconciliation(UnifiedImportResult result) {
        log.info("=== RECONCILIATION REPORT ===");
        
        if (result.getFitResult() != null) {
            ImportResult fit = result.getFitResult();
            log.info("FIT Files:");
            log.info("  - Total Processed: {}", fit.getTotalProcessed());
            log.info("  - Successfully Imported: {}", fit.getSuccessCount());
            log.info("  - Skipped (already processed): {}", fit.getSkippedCount());
            log.info("  - Failed: {}", fit.getFailedCount());
            
            if (fit.getFailedCount() > 0) {
                log.warn("  - Failed FIT files:");
                fit.getFailedFiles().forEach((file, error) -> 
                    log.warn("    * {}: {}", file, error));
            }
            
            String fitStatus = determineFitNextSteps(fit);
            log.info("  - Status: {}", fitStatus);
        } else {
            log.warn("FIT Files: NOT PROCESSED - Error: {}", result.getFitError());
        }

        if (result.getCsvResult() != null) {
            ImportResult csv = result.getCsvResult();
            log.info("CSV Files:");
            log.info("  - Total Processed: {}", csv.getTotalProcessed());
            log.info("  - Successfully Imported: {}", csv.getSuccessCount());
            log.info("  - Skipped (already in DB): {}", csv.getSkippedCount());
            log.info("  - Failed: {}", csv.getFailedCount());
            
            if (csv.getFailedCount() > 0) {
                log.warn("  - Failed CSV activities:");
                csv.getFailedFiles().forEach((activity, error) -> 
                    log.warn("    * {}: {}", activity, error));
            }
            
            String csvStatus = determineCsvNextSteps(csv);
            log.info("  - Status: {}", csvStatus);
        } else {
            log.warn("CSV Files: NOT PROCESSED - Error: {}", result.getCsvError());
        }

        int totalSuccess = result.getTotalSuccessCount();
        int totalSkipped = result.getTotalSkippedCount();
        int totalFailed = result.getTotalFailedCount();
        
        log.info("=== OVERALL SUMMARY ===");
        log.info("Total Imported: {}", totalSuccess);
        log.info("Total Skipped: {}", totalSkipped);
        log.info("Total Failed: {}", totalFailed);
        log.info("Total Duration: {}ms", result.getTotalDurationMs());
        
        String overallNextSteps = determineOverallNextSteps(result);
        log.info("=== NEXT STEPS ===");
        log.info(overallNextSteps);
    }

    private String determineFitNextSteps(ImportResult fit) {
        if (fit.getFailedCount() > 0) {
            return "ACTION REQUIRED: Review failed FIT files and fix issues";
        } else if (fit.getSuccessCount() > 0) {
            return "COMPLETE: All FIT files processed successfully";
        } else if (fit.getSkippedCount() > 0) {
            return "COMPLETE: All FIT files already processed";
        } else {
            return "COMPLETE: No FIT files found to process";
        }
    }

    private String determineCsvNextSteps(ImportResult csv) {
        if (csv.getFailedCount() > 0) {
            return "ACTION REQUIRED: Review failed CSV activities and fix issues";
        } else if (csv.getSuccessCount() > 0) {
            return "COMPLETE: All CSV activities processed successfully";
        } else if (csv.getSkippedCount() > 0) {
            return "COMPLETE: All CSV activities already in database";
        } else {
            return "COMPLETE: No CSV files found to process";
        }
    }

    private String determineOverallNextSteps(UnifiedImportResult result) {
        StringBuilder steps = new StringBuilder();
        
        boolean hasErrors = (result.getFitError() != null) || (result.getCsvError() != null);
        boolean hasFailed = result.getTotalFailedCount() > 0;
        
        if (hasErrors) {
            steps.append("1. CRITICAL: Fix import service errors\n");
            if (result.getFitError() != null) {
                steps.append("   - FIT Import Error: ").append(result.getFitError()).append("\n");
            }
            if (result.getCsvError() != null) {
                steps.append("   - CSV Import Error: ").append(result.getCsvError()).append("\n");
            }
        }
        
        if (hasFailed) {
            steps.append(hasErrors ? "2" : "1").append(". Review and fix failed file imports\n");
            if (result.getFitResult() != null && result.getFitResult().getFailedCount() > 0) {
                steps.append("   - Check FIT files for corruption or format issues\n");
            }
            if (result.getCsvResult() != null && result.getCsvResult().getFailedCount() > 0) {
                steps.append("   - Check CSV data for validation errors\n");
            }
        }
        
        if (!hasErrors && !hasFailed) {
            if (result.getTotalSuccessCount() > 0) {
                steps.append("✓ All files processed successfully\n");
                steps.append("✓ Data is ready for analysis\n");
                steps.append("✓ Next scheduled import will run automatically");
            } else if (result.getTotalSkippedCount() > 0) {
                steps.append("✓ All files already processed\n");
                steps.append("✓ No new data to import\n");
                steps.append("✓ System is up to date");
            } else {
                steps.append("✓ No files found to process\n");
                steps.append("✓ Add FIT or CSV files to import folders\n");
                steps.append("✓ Next scheduled import will process them");
            }
        }
        
        return steps.toString();
    }

    public void shutdown() {
        log.info("Shutting down UnifiedGarminImportService executor");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
