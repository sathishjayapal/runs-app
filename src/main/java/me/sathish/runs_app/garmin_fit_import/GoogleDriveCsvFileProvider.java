package me.sathish.runs_app.garmin_fit_import;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GoogleDriveCsvFileProvider implements CsvFileProvider {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String MIME_TEXT_CSV = "text/csv";
    private static final String MIME_GOOGLE_SHEET = "application/vnd.google-apps.spreadsheet";

    private final GarminCsvImportProperties properties;
    private final Drive drive;

    public GoogleDriveCsvFileProvider(GarminCsvImportProperties properties) {
        this.properties = properties;
        this.drive = initializeDrive(properties);
    }

    @Override
    public GarminCsvImportProperties.Source getSourceType() {
        return GarminCsvImportProperties.Source.DRIVE;
    }

    @Override
    public List<CsvFileHandle> listCsvFiles(String folderOverride) throws IOException {
        if (drive == null) {
            log.warn("Google Drive client is not initialized; returning empty file list");
            return List.of();
        }

        String folderId = Optional.ofNullable(folderOverride)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> Optional.ofNullable(properties.getDrive())
                        .map(GarminCsvImportProperties.Drive::getFolderId)
                        .orElse(null));

        if (folderId == null || folderId.isBlank()) {
            log.warn("Google Drive folder id is not configured");
            return List.of();
        }

        try {
            String query = String.format("'%s' in parents and trashed = false", folderId);
            FileList fileList = drive.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setSupportsAllDrives(true)
                    .setIncludeItemsFromAllDrives(true)
                    .setCorpora("allDrives")
                    .setFields("files(id,name,mimeType,fileExtension,modifiedTime,parents)")
                    .execute();

            if (fileList.getFiles() == null || fileList.getFiles().isEmpty()) {
                return List.of();
            }

            List<File> candidates = fileList.getFiles().stream()
                    .filter(this::isCsvCandidate)
                    .toList();

            if (candidates.isEmpty()) {
                log.info("No CSV candidates found in Drive folder {}. Files seen: {}", folderId,
                        fileList.getFiles().stream()
                                .map(f -> String.format("%s[%s]", f.getName(), f.getMimeType()))
                                .collect(Collectors.joining(", ")));
                return List.of();
            }

            return candidates.stream()
                    .sorted(Comparator.comparingLong(f -> f.getModifiedTime().getValue()))
                    .map(file -> new DriveCsvFileHandle(drive, file, properties.getDrive().getProcessedFolderId(), file.getMimeType()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list CSV files from Google Drive", e);
            throw e;
        }
    }

    private boolean isCsvCandidate(File file) {
        String name = Optional.ofNullable(file.getName()).orElse("");
        String mimeType = Optional.ofNullable(file.getMimeType()).orElse("");
        String extension = Optional.ofNullable(file.getFileExtension()).orElse("");

        return MIME_TEXT_CSV.equalsIgnoreCase(mimeType)
                || MIME_GOOGLE_SHEET.equalsIgnoreCase(mimeType)
                || "csv".equalsIgnoreCase(extension)
                || name.toLowerCase().endsWith(".csv");
    }

    private Drive initializeDrive(GarminCsvImportProperties properties) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredentials credentials = loadCredentials(properties);
            if (credentials == null) {
                log.warn("Google Drive service account credentials are not configured");
                return null;
            }

            return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName(Optional.ofNullable(properties.getDrive().getApplicationName())
                            .orElse("Runs App CSV Importer"))
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to initialize Google Drive client", e);
            return null;
        }
    }

    private GoogleCredentials loadCredentials(GarminCsvImportProperties properties) {
        GarminCsvImportProperties.Drive driveProps = properties.getDrive();
        if (driveProps == null) {
            return null;
        }

        try {
            if (driveProps.getServiceAccountKeyBase64() != null && !driveProps.getServiceAccountKeyBase64().isBlank()) {
                byte[] decoded = Base64.getDecoder()
                        .decode(driveProps.getServiceAccountKeyBase64().getBytes(StandardCharsets.UTF_8));
                return ServiceAccountCredentials.fromStream(new java.io.ByteArrayInputStream(decoded))
                        .createScoped(Collections.singleton(DriveScopes.DRIVE));
            }

            if (driveProps.getServiceAccountKeyPath() != null && !driveProps.getServiceAccountKeyPath().isBlank()) {
                Path path = Path.of(driveProps.getServiceAccountKeyPath());
                if (Files.exists(path)) {
                    try (InputStream in = new FileInputStream(path.toFile())) {
                        return ServiceAccountCredentials.fromStream(in)
                                .createScoped(Collections.singleton(DriveScopes.DRIVE));
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to load Google Drive service account credentials", e);
        }

        return null;
    }

    public boolean isDriveClientInitialized() {
        return drive != null;
    }

    /**
     * Lists ALL files in the given folder (no mimeType filter) for diagnostic
     * purposes.
     * Returns file name and mimeType so we can see what Google Drive thinks the
     * files are.
     */
    public List<Map<String, String>> listAllFilesInFolder(String folderId) throws IOException {
        if (drive == null) {
            throw new IOException("Google Drive client is not initialized");
        }
        if (folderId == null || folderId.isBlank()) {
            throw new IOException("Folder ID is not configured");
        }

        String query = String.format("'%s' in parents and trashed = false", folderId);
        FileList fileList = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name,mimeType,modifiedTime,size)")
                .execute();

        if (fileList.getFiles() == null) {
            return List.of();
        }

        return fileList.getFiles().stream()
                .map(f -> Map.of(
                        "name", String.valueOf(f.getName()),
                        "mimeType", String.valueOf(f.getMimeType()),
                        "id", String.valueOf(f.getId()),
                        "modifiedTime", String.valueOf(f.getModifiedTime())))
                .collect(Collectors.toList());
    }

    private static class DriveCsvFileHandle implements CsvFileHandle {
        private final Drive drive;
        private final File file;
        private final String processedFolderId;
        private final String mimeType;

        private DriveCsvFileHandle(Drive drive, File file, String processedFolderId, String mimeType) {
            this.drive = drive;
            this.file = file;
            this.processedFolderId = processedFolderId;
            this.mimeType = mimeType;
        }

        @Override
        public String getFileName() {
            return file.getName();
        }

        @Override
        public InputStream openStream() throws IOException {
            if (drive == null) {
                throw new IOException("Google Drive client not initialized");
            }
            if (MIME_GOOGLE_SHEET.equalsIgnoreCase(mimeType)) {
                // Google Sheets files must be exported to CSV before reading.
                return drive.files().export(file.getId(), MIME_TEXT_CSV).executeMediaAsInputStream();
            }
            return drive.files().get(file.getId()).executeMediaAsInputStream();
        }

        @Override
        public void markProcessed() throws IOException {
            if (processedFolderId == null || processedFolderId.isBlank()) {
                return;
            }

            if (drive == null) {
                throw new IOException("Google Drive client not initialized");
            }

            try {
                File current = drive.files().get(file.getId()).setFields("parents").execute();
                String removeParents = current.getParents() == null || current.getParents().isEmpty()
                        ? null
                        : String.join(",", current.getParents());

                drive.files().update(file.getId(), new File())
                        .setAddParents(processedFolderId)
                        .setRemoveParents(removeParents)
                        .execute();
            } catch (IOException e) {
                log.warn("Failed to move file {} to processed folder", file.getName(), e);
                throw e;
            }
        }
    }
}
