package me.sathish.runsapp.runs_app.file_name_tracker;

import java.util.List;


public interface FileNameTrackerService {

    List<FileNameTrackerDTO> findAll();

    FileNameTrackerDTO get(Long id);

    Long create(FileNameTrackerDTO fileNameTrackerDTO);

    void update(Long id, FileNameTrackerDTO fileNameTrackerDTO);

    void delete(Long id);

}
