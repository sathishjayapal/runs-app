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
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GoogleDriveCsvFileProvider implements CsvFileProvider {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

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
            String query = String.format("'%s' in parents and trashed = false and mimeType = 'text/csv'", folderId);
            FileList fileList = drive.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id,name,modifiedTime,parents)")
                    .execute();

            if (fileList.getFiles() == null || fileList.getFiles().isEmpty()) {
                return List.of();
            }

            return fileList.getFiles().stream()
                    .sorted(Comparator.comparingLong(f -> f.getModifiedTime().getValue()))
                    .map(file -> new DriveCsvFileHandle(drive, file, properties.getDrive().getProcessedFolderId()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list CSV files from Google Drive", e);
            throw e;
        }
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
                byte[] decoded = Base64.getDecoder().decode(driveProps.getServiceAccountKeyBase64().getBytes(StandardCharsets.UTF_8));
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

    private static class DriveCsvFileHandle implements CsvFileHandle {
        private final Drive drive;
        private final File file;
        private final String processedFolderId;

        private DriveCsvFileHandle(Drive drive, File file, String processedFolderId) {
            this.drive = drive;
            this.file = file;
            this.processedFolderId = processedFolderId;
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
