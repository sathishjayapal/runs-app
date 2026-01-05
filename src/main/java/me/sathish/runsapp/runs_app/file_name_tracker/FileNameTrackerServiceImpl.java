package me.sathish.runsapp.runs_app.file_name_tracker;

import me.sathish.runsapp.runs_app.events.BeforeDeleteRunAppUser;
import me.sathish.runsapp.runs_app.run_app_user.RunAppUser;
import me.sathish.runsapp.runs_app.run_app_user.RunAppUserRepository;
import me.sathish.runsapp.runs_app.util.NotFoundException;
import me.sathish.runsapp.runs_app.util.ReferencedException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class FileNameTrackerServiceImpl implements FileNameTrackerService {

    private final FileNameTrackerRepository fileNameTrackerRepository;
    private final RunAppUserRepository runAppUserRepository;

    public FileNameTrackerServiceImpl(final FileNameTrackerRepository fileNameTrackerRepository,
            final RunAppUserRepository runAppUserRepository) {
        this.fileNameTrackerRepository = fileNameTrackerRepository;
        this.runAppUserRepository = runAppUserRepository;
    }

    @Override
    public Page<FileNameTrackerDTO> findAll(final String filter, final Pageable pageable) {
        Page<FileNameTracker> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = fileNameTrackerRepository.findAllById(longFilter, pageable);
        } else {
            page = fileNameTrackerRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(fileNameTracker -> mapToDTO(fileNameTracker, new FileNameTrackerDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    @Override
    public FileNameTrackerDTO get(final Long id) {
        return fileNameTrackerRepository.findById(id)
                .map(fileNameTracker -> mapToDTO(fileNameTracker, new FileNameTrackerDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Long create(final FileNameTrackerDTO fileNameTrackerDTO) {
        final FileNameTracker fileNameTracker = new FileNameTracker();
        mapToEntity(fileNameTrackerDTO, fileNameTracker);
        return fileNameTrackerRepository.save(fileNameTracker).getId();
    }

    @Override
    public void update(final Long id, final FileNameTrackerDTO fileNameTrackerDTO) {
        final FileNameTracker fileNameTracker = fileNameTrackerRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(fileNameTrackerDTO, fileNameTracker);
        fileNameTrackerRepository.save(fileNameTracker);
    }

    @Override
    public void delete(final Long id) {
        final FileNameTracker fileNameTracker = fileNameTrackerRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        fileNameTrackerRepository.delete(fileNameTracker);
    }

    private FileNameTrackerDTO mapToDTO(final FileNameTracker fileNameTracker,
            final FileNameTrackerDTO fileNameTrackerDTO) {
        fileNameTrackerDTO.setId(fileNameTracker.getId());
        fileNameTrackerDTO.setFileName(fileNameTracker.getFileName());
        fileNameTrackerDTO.setUpdatedBy(fileNameTracker.getUpdatedBy());
        fileNameTrackerDTO.setCreatedBy(fileNameTracker.getCreatedBy() == null ? null : fileNameTracker.getCreatedBy().getId());
        return fileNameTrackerDTO;
    }

    private FileNameTracker mapToEntity(final FileNameTrackerDTO fileNameTrackerDTO,
            final FileNameTracker fileNameTracker) {
        fileNameTracker.setFileName(fileNameTrackerDTO.getFileName());
        fileNameTracker.setUpdatedBy(fileNameTrackerDTO.getUpdatedBy());
        final RunAppUser createdBy = fileNameTrackerDTO.getCreatedBy() == null ? null : runAppUserRepository.findById(fileNameTrackerDTO.getCreatedBy())
                .orElseThrow(() -> new NotFoundException("createdBy not found"));
        fileNameTracker.setCreatedBy(createdBy);
        return fileNameTracker;
    }

    @EventListener(BeforeDeleteRunAppUser.class)
    public void on(final BeforeDeleteRunAppUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final FileNameTracker createdByFileNameTracker = fileNameTrackerRepository.findFirstByCreatedById(event.getId());
        if (createdByFileNameTracker != null) {
            referencedException.setKey("runAppUser.fileNameTracker.createdBy.referenced");
            referencedException.addParam(createdByFileNameTracker.getId());
            throw referencedException;
        }
    }

}
