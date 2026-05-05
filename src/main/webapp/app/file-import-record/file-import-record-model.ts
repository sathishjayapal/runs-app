export interface FileImportRecord {
  id: number;
  fileName: string;
  expectedRows: number;
  successCount: number;
  failedCount: number;
  skippedCount: number;
  status: string;
  reconciliationStatus: string;
  reconciliationReport?: string;
  processedAt: string;
  completedAt: string;
}
