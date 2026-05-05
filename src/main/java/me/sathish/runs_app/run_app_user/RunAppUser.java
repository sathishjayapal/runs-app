package me.sathish.runs_app.run_app_user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import me.sathish.runs_app.file_import_record.FileImportRecord;
import me.sathish.runs_app.garmin_run.GarminRun;
import me.sathish.runs_app.strava_run.StravaRun;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class RunAppUser {

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

    @OneToMany(mappedBy = "createdBy")
    private Set<GarminRun> createdByGarminRuns = new HashSet<>();

    @OneToMany(mappedBy = "createdBy")
    private Set<FileImportRecord> createdByFileNameTrackers = new HashSet<>();

    @OneToMany(mappedBy = "createdBy")
    private Set<FileImportRecord> updatedByFileNameTrackers = new HashSet<>();

    @OneToMany(mappedBy = "createdBy")
    private Set<StravaRun> createdByStravaRuns = new HashSet<>();

    @OneToMany(mappedBy = "updateBy")
    private Set<GarminRun> updatedByGarminRuns = new HashSet<>();

    @OneToMany(mappedBy = "updatedBy")
    private Set<StravaRun> updatedByStravaRun = new HashSet<>();

    @ManyToMany(cascade = {jakarta.persistence.CascadeType.MERGE})
    @JoinTable(
            name = "run_app_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<me.sathish.runs_app.runner_app_role.RunnerAppRole> roles = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

}
