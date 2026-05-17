package me.sathish.runs_app.rifl;

import java.time.Instant;

public record CompletionRecord(int httpStatus, String bodyJson, Instant createdAt) {
}
