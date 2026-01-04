package me.sathish.runsapp.runs_app.strava_run;

import me.sathish.runsapp.runs_app.events.BeforeDeleteUser;
import me.sathish.runsapp.runs_app.user.User;
import me.sathish.runsapp.runs_app.user.UserRepository;
import me.sathish.runsapp.runs_app.util.NotFoundException;
import me.sathish.runsapp.runs_app.util.ReferencedException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class StravaRunServiceImpl implements StravaRunService {

    private final StravaRunRepository stravaRunRepository;
    private final UserRepository userRepository;

    public StravaRunServiceImpl(final StravaRunRepository stravaRunRepository,
            final UserRepository userRepository) {
        this.stravaRunRepository = stravaRunRepository;
        this.userRepository = userRepository;
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
        stravaRunDTO.setCreatedAt(stravaRun.getCreatedAt());
        stravaRunDTO.setUpdatedAt(stravaRun.getUpdatedAt());
        stravaRunDTO.setUpdatedBy(stravaRun.getCreatedBy() == null ? null : stravaRun.getCreatedBy().getId());
        stravaRunDTO.setCreatedBy(stravaRun.getCreatedBy() == null ? null : stravaRun.getCreatedBy().getId());
        return stravaRunDTO;
    }

    private StravaRun mapToEntity(final StravaRunDTO stravaRunDTO, final StravaRun stravaRun) {
        stravaRun.setCustomerId(stravaRunDTO.getCustomerId());
        stravaRun.setRunName(stravaRunDTO.getRunName());
        stravaRun.setRunDate(stravaRunDTO.getRunDate());
        stravaRun.setMiles(stravaRunDTO.getMiles());
        stravaRun.setStartLocation(stravaRunDTO.getStartLocation());
        stravaRun.setCreatedAt(stravaRunDTO.getCreatedAt());
        stravaRun.setUpdatedAt(stravaRunDTO.getUpdatedAt());
        final User updatedBy = stravaRunDTO.getUpdatedBy() == null ? null : userRepository.findById(stravaRunDTO.getUpdatedBy())
                .orElseThrow(() -> new NotFoundException("updatedBy not found"));
        stravaRun.setUpdatedBy(updatedBy);
        final User createdBy = stravaRunDTO.getCreatedBy() == null ? null : userRepository.findById(stravaRunDTO.getCreatedBy())
                .orElseThrow(() -> new NotFoundException("createdBy not found"));
        stravaRun.setCreatedBy(createdBy);
        return stravaRun;
    }

    @EventListener(BeforeDeleteUser.class)
    public void on(final BeforeDeleteUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final StravaRun createdByStravaRun = stravaRunRepository.findFirstByCreatedById(event.getId());
        if (createdByStravaRun != null) {
            referencedException.setKey("user.stravaRun.createdBy.referenced");
            referencedException.addParam(createdByStravaRun.getRunNumber());
            throw referencedException;
        }
    }

}
