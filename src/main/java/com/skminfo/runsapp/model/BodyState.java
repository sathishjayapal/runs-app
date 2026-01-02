package com.skminfo.runsapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BodyState {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private Integer painLevel;
    
    @Column(nullable = false)
    private Integer sleepQuality;
    
    @Column(nullable = false)
    private Integer stressLevel;
    
    private String notes;
    
    public BodyState(Integer painLevel, Integer sleepQuality, Integer stressLevel) {
        this.timestamp = LocalDateTime.now();
        this.painLevel = painLevel;
        this.sleepQuality = sleepQuality;
        this.stressLevel = stressLevel;
    }
}
