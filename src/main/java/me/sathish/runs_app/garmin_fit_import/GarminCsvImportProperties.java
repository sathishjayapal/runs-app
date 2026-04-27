package me.sathish.runs_app.garmin_fit_import;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.garmin.import")
public class GarminCsvImportProperties {

    private String folder;
    private String csvFolder;
    private Source source = Source.LOCAL;
    private Drive drive = new Drive();
    private Long systemUserId;
    private Alert alert = new Alert();

    public enum Source {
        LOCAL,
        DRIVE
    }

    @Getter
    @Setter
    public static class Drive {
        private String folderId;
        private String processedFolderId;
        private String quarantineFolderId;
        private String failedFolderId;
        private String retryFolderId;
        private String serviceAccountKeyPath;
        private String serviceAccountKeyBase64;
        private String applicationName;
    }

    @Getter
    @Setter
    public static class Alert {
        private AlertEmail email = new AlertEmail();

        @Getter
        @Setter
        public static class AlertEmail {
            private boolean enabled = true;
            private int maxRetryAttempts = 3;
            private String recipients = "";  // comma-separated
            private String fromAddress = "garmin-import@example.com";

            public List<String> getRecipientsList() {
                return Arrays.stream(recipients.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            }

            public boolean hasRecipients() {
                return !getRecipientsList().isEmpty();
            }
        }
    }
}
