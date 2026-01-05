package me.sathish.runsapp.runs_app.garmin_run;

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
public class GarminRunServiceImpl implements GarminRunService {

    private final GarminRunRepository garminRunRepository;
    private final UserRepository userRepository;

    public GarminRunServiceImpl(final GarminRunRepository garminRunRepository,
            final UserRepository userRepository) {
        this.garminRunRepository = garminRunRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<GarminRunDTO> findAll(final String filter, final Pageable pageable) {
        Page<GarminRun> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = garminRunRepository.findAllById(longFilter, pageable);
        } else {
            page = garminRunRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(garminRun -> mapToDTO(garminRun, new GarminRunDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    @Override
    public GarminRunDTO get(final Long id) {
        return garminRunRepository.findById(id)
                .map(garminRun -> mapToDTO(garminRun, new GarminRunDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Long create(final GarminRunDTO garminRunDTO) {
        final GarminRun garminRun = new GarminRun();
        mapToEntity(garminRunDTO, garminRun);
        return garminRunRepository.save(garminRun).getId();
    }

    @Override
    public void update(final Long id, final GarminRunDTO garminRunDTO) {
        final GarminRun garminRun = garminRunRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(garminRunDTO, garminRun);
        garminRunRepository.save(garminRun);
    }

    @Override
    public void delete(final Long id) {
        final GarminRun garminRun = garminRunRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        garminRunRepository.delete(garminRun);
    }

    private GarminRunDTO mapToDTO(final GarminRun garminRun, final GarminRunDTO garminRunDTO) {
        garminRunDTO.setId(garminRun.getId());
        garminRunDTO.setActivityId(garminRun.getActivityId());
        garminRunDTO.setActivityDate(garminRun.getActivityDate());
        garminRunDTO.setActivityType(garminRun.getActivityType());
        garminRunDTO.setActivityName(garminRun.getActivityName());
        garminRunDTO.setActivityDescription(garminRun.getActivityDescription());
        garminRunDTO.setElapsedTime(garminRun.getElapsedTime());
        garminRunDTO.setDistance(garminRun.getDistance());
        garminRunDTO.setMaxHeartRate(garminRun.getMaxHeartRate());
        garminRunDTO.setCalories(garminRun.getCalories());
        garminRunDTO.setUpdatedBy(garminRun.getUpdatedBy());
        garminRunDTO.setCreatedBy(garminRun.getCreatedBy() == null ? null : garminRun.getCreatedBy().getId());
        return garminRunDTO;
    }

    private GarminRun mapToEntity(final GarminRunDTO garminRunDTO, final GarminRun garminRun) {
        garminRun.setActivityId(garminRunDTO.getActivityId());
        garminRun.setActivityDate(garminRunDTO.getActivityDate());
        garminRun.setActivityType(garminRunDTO.getActivityType());
        garminRun.setActivityName(garminRunDTO.getActivityName());
        garminRun.setActivityDescription(garminRunDTO.getActivityDescription());
        garminRun.setElapsedTime(garminRunDTO.getElapsedTime());
        garminRun.setDistance(garminRunDTO.getDistance());
        garminRun.setMaxHeartRate(garminRunDTO.getMaxHeartRate());
        garminRun.setCalories(garminRunDTO.getCalories());
        garminRun.setUpdatedBy(garminRunDTO.getUpdatedBy());
        final User createdBy = garminRunDTO.getCreatedBy() == null ? null : userRepository.findById(garminRunDTO.getCreatedBy())
                .orElseThrow(() -> new NotFoundException("createdBy not found"));
        garminRun.setCreatedBy(createdBy);
        return garminRun;
    }

    @EventListener(BeforeDeleteUser.class)
    public void on(final BeforeDeleteUser event) {
        final ReferencedException referencedException = new ReferencedException();
        final GarminRun createdByGarminRun = garminRunRepository.findFirstByCreatedById(event.getId());
        if (createdByGarminRun != null) {
            referencedException.setKey("user.garminRun.createdBy.referenced");
            referencedException.addParam(createdByGarminRun.getId());
            throw referencedException;
        }
    }

}
