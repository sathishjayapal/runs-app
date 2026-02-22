package me.sathish.runs_app.file_name_tracker;

import java.util.List;
import me.sathish.runs_app.run_app_user.RunAppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface FileNameTrackerRepository extends JpaRepository<FileNameTracker, Long> {

    Page<FileNameTracker> findAllById(Long id, Pageable pageable);

    FileNameTracker findFirstByCreatedById(Long id);

    boolean existsByFileName(String fileName);

    FileNameTracker findByFileName(String fileName);

    List<FileNameTracker> findByCreatedBy(RunAppUser createdBy);

    long countByCreatedBy(RunAppUser createdBy);

}
