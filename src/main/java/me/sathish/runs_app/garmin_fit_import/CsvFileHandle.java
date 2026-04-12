package me.sathish.runs_app.garmin_fit_import;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstraction for a CSV source that can provide an {@link InputStream} for parsing and
 * optionally perform clean-up (for example move the file to a processed location).
 */
public interface CsvFileHandle {

    String getFileName();

    InputStream openStream() throws IOException;

    default void markProcessed() throws IOException {
        // no-op by default
    }
}
