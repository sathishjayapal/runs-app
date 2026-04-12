package me.sathish.runs_app.garmin_fit_import;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.garmin.import")
public class GarminCsvImportProperties {

    private String folder;
    private String csvFolder;
    private Source source = Source.LOCAL;
    private Drive drive = new Drive();
    private Long systemUserId;

    public enum Source {
        LOCAL,
        DRIVE
    }

    @Getter
    @Setter
    public static class Drive {
        private String folderId;
        private String processedFolderId;
        private String serviceAccountKeyPath;
        private String serviceAccountKeyBase64;
        private String applicationName;
    }
}
