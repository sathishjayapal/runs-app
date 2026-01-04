package me.sathish.runsapp.runs_app.file_name_tracker;

import org.springframework.data.jpa.repository.JpaRepository;


public interface FileNameTrackerRepository extends JpaRepository<FileNameTracker, Long> {

    FileNameTracker findFirstByCreatedById(Long id);

}
