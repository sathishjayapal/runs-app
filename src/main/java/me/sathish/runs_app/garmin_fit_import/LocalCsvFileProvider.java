package me.sathish.runs_app.garmin_fit_import;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocalCsvFileProvider implements CsvFileProvider {

    private final GarminCsvImportProperties properties;

    @Override
    public GarminCsvImportProperties.Source getSourceType() {
        return GarminCsvImportProperties.Source.LOCAL;
    }

    @Override
    public List<CsvFileHandle> listCsvFiles(String folderOverride) {
        String folder = (folderOverride != null && !folderOverride.isBlank())
                ? folderOverride
                : properties.getCsvFolder();

        File dir = new File(folder);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("CSV import folder does not exist or is not a directory: {}", folder);
            return List.of();
        }

        File[] csvFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null || csvFiles.length == 0) {
            log.info("No CSV files found in folder: {}", folder);
            return List.of();
        }

        return Arrays.stream(csvFiles)
                .sorted(Comparator.comparing(java.io.File::getName))
                .map(LocalCsvFileHandle::new)
                .collect(Collectors.toList());
    }

    private static class LocalCsvFileHandle implements CsvFileHandle {
        private final File file;

        LocalCsvFileHandle(File file) {
            this.file = file;
        }

        @Override
        public String getFileName() {
            return file.getName();
        }

        @Override
        public InputStream openStream() throws IOException {
            return new FileInputStream(file);
        }
    }
}
