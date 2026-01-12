package me.sathish.runs_app.garmin_run;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import me.sathish.runs_app.run_app_user.RunAppUser;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class GarminRun {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(
            name = "primary_sequence",
            sequenceName = "primary_sequence",
            allocationSize = 1,
            initialValue = 10000
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "primary_sequence"
    )
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String activityId;

    @Column(nullable = false, columnDefinition = "text")
    private String activityDate;

    @Column(nullable = false, columnDefinition = "text")
    private String activityType;

    @Column(nullable = false, columnDefinition = "text")
    private String activityName;

    @Column(columnDefinition = "text")
    private String activityDescription;

    @Column(columnDefinition = "text")
    private String elapsedTime;

    @Column(nullable = false, columnDefinition = "text")
    private String distance;

    @Column(columnDefinition = "text")
    private String maxHeartRate;

    @Column(columnDefinition = "text")
    private String calories;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private RunAppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "update_by_id")
    private RunAppUser updateBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

}
