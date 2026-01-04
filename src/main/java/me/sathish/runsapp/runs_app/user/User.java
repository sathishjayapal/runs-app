package me.sathish.runsapp.runs_app.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.sathish.runsapp.runs_app.file_name_tracker.FileNameTracker;
import me.sathish.runsapp.runs_app.garmin_run.GarminRun;
import me.sathish.runsapp.runs_app.role.Role;
import me.sathish.runsapp.runs_app.strava_run.StravaRun;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "\"user\"")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class User {

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

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "createdBy")
    private Set<GarminRun> createdByGarminRunses = new HashSet<>();

    @OneToMany(mappedBy = "createdBy")
    private Set<FileNameTracker> createdByFileNameTrackers = new HashSet<>();

    @OneToMany(mappedBy = "createdBy")
    private Set<StravaRun> createdByStravaRunses = new HashSet<>();

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
