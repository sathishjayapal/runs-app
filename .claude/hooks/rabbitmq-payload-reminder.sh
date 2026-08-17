#!/usr/bin/env bash
# PreToolUse reminder for Edit/Write on the two files where the 2026-08-16 silent-message-drop
# incident originated. Never blocks — see memory/rabbitmq_cross_service_payload_type.md for the
# rule this exists to keep visible: rabbitTemplate.convertAndSend payloads for queues consumed by
# a different service (eventstracker, runs-ai-analyzer) must be JSON strings via
# objectMapper.writeValueAsString(...), never raw objects.
set -euo pipefail

input="$(cat)"
file_path="$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty')"

case "$file_path" in
  */garmin_fit_import/GarminCsvImportService.java|*/config/RabbitMQConfiguration.java)
    jq -n '{
      systemMessage: "RabbitMQ cross-service payload rule: rabbitTemplate.convertAndSend payloads for queues consumed by a different service (eventstracker, runs-ai-analyzer) must be JSON strings via objectMapper.writeValueAsString(...) — never raw objects. A raw object gets a __TypeId__ header stamped with the producer FQN that the consumer cannot resolve on its classpath, and the message is silently dropped with no error visible in this app. All Garmin event publishes should go through GarminCsvImportService.publishGarminEvent, the single sanctioned call site. See memory/rabbitmq_cross_service_payload_type.md and the regression test GarminCsvImportServicePublishTest.",
      hookSpecificOutput: {
        hookEventName: "PreToolUse",
        permissionDecision: "allow",
        permissionDecisionReason: "Informational reminder only; edit is not blocked."
      }
    }'
    ;;
  *)
    exit 0
    ;;
esac
