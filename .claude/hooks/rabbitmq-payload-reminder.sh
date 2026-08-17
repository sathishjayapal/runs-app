#!/usr/bin/env bash
# PreToolUse reminder for Edit/Write on the two files where the 2026-08-16/17 silent-message-drop
# incidents originated (two distinct bugs, back to back — see memory/rabbitmq_cross_service_payload_type.md
# for the full writeup). Never blocks.
#
# The rule: for queues consumed by a DIFFERENT service, never call rabbitTemplate.convertAndSend at
# all — not with a raw object, and not even with a pre-serialized JSON String (the converter
# re-serializes a String too, double-encoding it, which breaks any consumer that manually parses
# the raw Message body). Bypass the converter entirely: build the Message by hand and call
# rabbitTemplate.send(exchange, routingKey, message).
set -euo pipefail

input="$(cat)"
file_path="$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty')"

case "$file_path" in
  */garmin_fit_import/GarminCsvImportService.java|*/config/RabbitMQConfiguration.java)
    jq -n '{
      systemMessage: "RabbitMQ cross-service payload rule: for queues consumed by a different service (eventstracker, runs-ai-analyzer), never call rabbitTemplate.convertAndSend — not with a raw object, and not even with a pre-serialized JSON String. RabbitTemplate'"'"'s converter re-serializes whatever it'"'"'s given, so a String payload gets double-encoded on the wire, breaking any consumer that manually parses the raw Message body (as eventstracker'"'"'s Garmin/GitHub listeners do). Bypass the converter entirely: build the Message by hand (MessageBuilder.withBody(objectMapper.writeValueAsString(event).getBytes(UTF_8))...) and call rabbitTemplate.send(exchange, routingKey, message), not convertAndSend. All Garmin event publishes should go through GarminCsvImportService.publishGarminEvent, the single sanctioned call site (it branches on routing key for one deliberate, verified exception — read its javadoc before changing it). See memory/rabbitmq_cross_service_payload_type.md and the regression test GarminCsvImportServicePublishTest.",
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
