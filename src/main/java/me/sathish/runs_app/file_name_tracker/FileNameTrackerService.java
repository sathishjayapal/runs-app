package me.sathish.runs_app.file_name_tracker;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface FileNameTrackerService {

    Page<FileNameTrackerDTO> findAll(String filter, Pageable pageable);

    FileNameTrackerDTO get(Long id);

    Long create(FileNameTrackerDTO fileNameTrackerDTO);

    void update(Long id, FileNameTrackerDTO fileNameTrackerDTO);

    void delete(Long id);

}
