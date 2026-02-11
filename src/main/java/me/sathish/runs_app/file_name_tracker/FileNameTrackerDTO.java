package me.sathish.runs_app.file_name_tracker;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class FileNameTrackerDTO {

    private Long id;

    @NotNull
    private String fileName;

    @Size(max = 255)
    private String updatedBy;

    @NotNull
    private Long createdBy;

    private String createdByName;
    private String createdAt;

}
