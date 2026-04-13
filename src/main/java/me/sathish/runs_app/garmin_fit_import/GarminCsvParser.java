package me.sathish.runs_app.garmin_fit_import;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class GarminCsvParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Column indices in Garmin activities.csv export
    private static final int COL_ACTIVITY_TYPE = 0;
    private static final int COL_DATE          = 1;
    private static final int COL_TITLE         = 3;
    private static final int COL_DISTANCE      = 4;
    private static final int COL_CALORIES      = 5;
    private static final int COL_TIME          = 6;
    private static final int COL_AVG_HR        = 7;
    private static final int COL_MAX_HR        = 8;
    private static final int MIN_COLUMNS       = 9;

    public List<FitActivityData> parse(InputStream inputStream, String sourceName) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return parse(reader, sourceName);
        }
    }

    private List<FitActivityData> parse(Reader reader, String sourceName) throws IOException {
        List<FitActivityData> activities = new ArrayList<>();

        try (BufferedReader bufferedReader = toBuffered(reader)) {
            String header = bufferedReader.readLine();
            if (header == null) {
                return activities;
            }

            String line;
            int lineNumber = 1;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                try {
                    FitActivityData data = parseLine(line);
                    if (data != null) {
                        activities.add(data);
                    }
                } catch (Exception e) {
                    log.warn("Skipping unparseable CSV row at line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        log.info("Parsed {} activities from {}", activities.size(), sourceName);
        return activities;
    }

    private BufferedReader toBuffered(Reader reader) {
        return reader instanceof BufferedReader buffered ? buffered : new BufferedReader(reader);
    }

    private FitActivityData parseLine(String line) {
        String[] columns = splitCsvLine(line);
        if (columns.length < MIN_COLUMNS) {
            return null;
        }

        String dateStr = clean(columns[COL_DATE]);
        if (dateStr.isEmpty()) {
            return null;
        }

        LocalDateTime activityDate = LocalDateTime.parse(dateStr, DATE_FORMAT);

        FitActivityData data = new FitActivityData();
        data.setActivityId(String.valueOf(activityDate.toEpochSecond(ZoneOffset.UTC)));
        data.setActivityDate(activityDate.toString());
        data.setActivityType(normalizeActivityType(clean(columns[COL_ACTIVITY_TYPE])));
        data.setActivityName(clean(columns[COL_TITLE]));
        data.setDistanceMiles(parseDouble(clean(columns[COL_DISTANCE])));
        data.setElapsedTimeSeconds(parseElapsedSeconds(clean(columns[COL_TIME])));
        data.setCalories(parseInteger(clean(columns[COL_CALORIES])));
        data.setAvgHeartRate(parseInteger(clean(columns[COL_AVG_HR])));
        data.setMaxHeartRate(parseInteger(clean(columns[COL_MAX_HR])));
        return data;
    }

    /**
     * Splits a CSV line respecting double-quoted fields that may contain commas.
     */
    private String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    private String clean(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("^\"|\"$", "").trim();
    }

    private String normalizeActivityType(String csvType) {
        String lower = csvType.toLowerCase();
        if (lower.contains("strength")) return "strength_training";
        if (lower.contains("elliptical")) return "elliptical";
        return "running";
    }

    private Double parseDouble(String value) {
        if (value.isEmpty() || "--".equals(value)) return null;
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (value.isEmpty() || "--".equals(value)) return null;
        try {
            return Integer.parseInt(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses elapsed time in HH:MM:SS or HH:MM:SS.s format into total seconds.
     */
    private Integer parseElapsedSeconds(String value) {
        if (value.isEmpty() || "--".equals(value)) return null;
        try {
            String[] parts = value.split(":");
            if (parts.length != 3) return null;
            int hours   = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = (int) Double.parseDouble(parts[2]);
            return hours * 3600 + minutes * 60 + seconds;
        } catch (Exception e) {
            log.warn("Could not parse elapsed time '{}': {}", value, e.getMessage());
            return null;
        }
    }
}
