package com.skminfo.runsapp.dto;

import com.skminfo.runsapp.model.TrainingSession.SessionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSessionRequest {
    private SessionType plannedType;
    private Double plannedDistance;
    private LocalDateTime plannedDate;
}
