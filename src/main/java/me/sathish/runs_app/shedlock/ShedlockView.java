package me.sathish.runs_app.shedlock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;


@Entity
@Table(name = "shedlock")
@Immutable
@Getter
@Setter
public class ShedlockView {

    @Id
    @Column(nullable = false, updatable = false, length = 64)
    private String name;

    @Column(nullable = false, name = "lock_until")
    private LocalDateTime lockUntil;

    @Column(nullable = false, name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(nullable = false, name = "locked_by", length = 255)
    private String lockedBy;

}
