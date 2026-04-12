package me.sathish.runs_app.garmin_fit_import;

import java.io.IOException;
import java.util.List;

/**
 * Abstraction over a source of Garmin CSV files.
 */
public interface CsvFileProvider {

    GarminCsvImportProperties.Source getSourceType();

    List<CsvFileHandle> listCsvFiles(String folderOverride) throws IOException;
}
