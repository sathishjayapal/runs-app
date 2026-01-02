package com.skminfo.runsapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BodyStateRequest {
    private Integer painLevel;
    private Integer sleepQuality;
    private Integer stressLevel;
    private String notes;
}
