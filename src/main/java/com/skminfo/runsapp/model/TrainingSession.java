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
public class TrainingSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime plannedDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType plannedType;
    
    @Column(nullable = false)
    private Double plannedDistance;
    
    @Enumerated(EnumType.STRING)
    private SessionType recommendedType;
    
    private Double recommendedDistance;
    
    @Column(length = 1000)
    private String recommendation;
    
    @ManyToOne
    @JoinColumn(name = "body_state_id")
    private BodyState bodyState;
    
    private Boolean completed;
    
    public enum SessionType {
        LONG_RUN,
        EASY_RUN,
        TEMPO_RUN,
        INTERVAL_RUN,
        RECOVERY_RUN,
        CROSS_TRAINING,
        REST
    }
}
