package me.sathish.runs_app.strava_run;

import me.sathish.runs_app.events.BeforeDeleteRunAppUser;
import me.sathish.runs_app.run_app_user.RunAppUser;
import me.sathish.runs_app.run_app_user.RunAppUserRepository;
import me.sathish.runs_app.util.NotFoundException;
import me.sathish.runs_app.util.ReferencedException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class StravaRunServiceImpl implements StravaRunService {

    private final StravaRunRepository stravaRunRepository;
    private final RunAppUserRepository runAppUserRepository;

    public StravaRunServiceImpl(final StravaRunRepository stravaRunRepository,
            final RunAppUserRepository runAppUserRepository) {
        this.stravaRunRepository = stravaRunRepository;
        this.runAppUserRepository = runAppUserRepository;
    }

    @Override
    public Page<StravaRunDTO> findAll(final String filter, final Pageable pageable) {
        Page<StravaRun> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = stravaRunRepository.findAllByRunNumber(longFilter, pageable);
        } else {
            page = stravaRunRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(stravaRun -> mapToDTO(stravaRun, new StravaRunDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    @Override
    public StravaRunDTO get(final Long runNumber) {
        return stravaRunRepository.findById(runNumber)
                .map(stravaRun -> mapToDTO(stravaRun, new StravaRunDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Long create(final StravaRunDTO stravaRunDTO) {
        final StravaRun stravaRun = new StravaRun();
        mapToEntity(stravaRunDTO, stravaRun);
        return stravaRunRepository.save(stravaRun).getRunNumber();
    }

    @Override
    public void update(final Long runNumber, final StravaRunDTO stravaRunDTO) {
        final StravaRun stravaRun = stravaRunRepository.findById(runNumber)
                .orElseThrow(NotFoundException::new);
        mapToEntity(stravaRunDTO, stravaRun);
        stravaRunRepository.save(stravaRun);
    }

    @Override
    public void delete(final Long runNumber) {
        final StravaRun stravaRun = stravaRunRepository.findById(runNumber)
                .orElseThrow(NotFoundException::new);
        stravaRunRepository.delete(stravaRun);
    }

    private StravaRunDTO mapToDTO(final StravaRun stravaRun, final StravaRunDTO stravaRunDTO) {
        stravaRunDTO.setRunNumber(stravaRun.getRunNumber());
        stravaRunDTO.setCustomerId(stravaRun.getCustomerId());
        stravaRunDTO.setRunName(stravaRun.getRunName());
        stravaRunDTO.setRunDate(stravaRun.getRunDate());
        stravaRunDTO.setMiles(stravaRun.getMiles());
        stravaRunDTO.setStartLocation(stravaRun.getStartLocation());
        stravaRunDTO.setCreatedBy(stravaRun.getCreatedBy() == null ? null : stravaRun.getCreatedBy().getId());
        stravaRunDTO.setUpdatedBy(stravaRun.getUpdatedBy() == null ? null : stravaRun.getUpdatedBy().getId());
        return stravaRunDTO;
    }

    private StravaRun mapToEntity(final StravaRunDTO stravaRunDTO, final StravaRun stravaRun) {
        stravaRun.setCustomerId(stravaRunDTO.getCustomerId());
        stravaRun.setRunName(stravaRunDTO.getRunName());
        stravaRun.setRunDate(stravaRunDTO.getRunDate());
        stravaRun.setMiles(stravaRunDTO.getMiles());
        stravaRun.setStartLocation(stravaRunDTO.getStartLocation());
        final RunAppUser createdBy = stravaRunDTO.getCreatedBy() == null ? null : runAppUserRepository.findById(stravaRunDTO.getCreatedBy())
                .orElseThrow(() -> new NotFoundException("createdBy not found"));
        stravaRun.setCreatedBy(createdBy);
        final RunAppUser updatedBy = stravaRunDTO.getUpdatedBy() == null ? null : runAppUserRepository.findById(stravaRunDTO.getUpdatedBy())
                .orElseThrow(() -> new NotFoundException("updatedBy not found"));
        stravaRun.setUpdatedBy(updatedBy);
        return stravaRun;
    }

    @EventListener(BeforeDeleteRunAppUser.class)
    public void on(final BeforeDeleteRunAppUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final StravaRun createdByStravaRun = stravaRunRepository.findFirstByCreatedById(event.getId());
        if (createdByStravaRun != null) {
            referencedException.setKey("runAppUser.stravaRun.createdBy.referenced");
            referencedException.addParam(createdByStravaRun.getRunNumber());
            throw referencedException;
        }
        final StravaRun updatedByStravaRun = stravaRunRepository.findFirstByUpdatedById(event.getId());
        if (updatedByStravaRun != null) {
            referencedException.setKey("runAppUser.stravaRun.updatedBy.referenced");
            referencedException.addParam(updatedByStravaRun.getRunNumber());
            throw referencedException;
        }
    }

}
