package me.sathish.runsapp.runs_app.shedlock;

import java.util.List;
import me.sathish.runsapp.runs_app.util.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class ShedlockServiceImpl implements ShedlockService {

    private final ShedlockRepository shedlockRepository;

    public ShedlockServiceImpl(final ShedlockRepository shedlockRepository) {
        this.shedlockRepository = shedlockRepository;
    }

    @Override
    public List<ShedlockDTO> findAll() {
        final List<Shedlock> shedlocks = shedlockRepository.findAll(Sort.by("name"));
        return shedlocks.stream()
                .map(shedlock -> mapToDTO(shedlock, new ShedlockDTO()))
                .toList();
    }

    @Override
    public ShedlockDTO get(final Long name) {
        return shedlockRepository.findById(name)
                .map(shedlock -> mapToDTO(shedlock, new ShedlockDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Long create(final ShedlockDTO shedlockDTO) {
        final Shedlock shedlock = new Shedlock();
        mapToEntity(shedlockDTO, shedlock);
        return shedlockRepository.save(shedlock).getName();
    }

    @Override
    public void update(final Long name, final ShedlockDTO shedlockDTO) {
        final Shedlock shedlock = shedlockRepository.findById(name)
                .orElseThrow(NotFoundException::new);
        mapToEntity(shedlockDTO, shedlock);
        shedlockRepository.save(shedlock);
    }

    @Override
    public void delete(final Long name) {
        final Shedlock shedlock = shedlockRepository.findById(name)
                .orElseThrow(NotFoundException::new);
        shedlockRepository.delete(shedlock);
    }

    private ShedlockDTO mapToDTO(final Shedlock shedlock, final ShedlockDTO shedlockDTO) {
        shedlockDTO.setName(shedlock.getName());
        shedlockDTO.setLockUntil(shedlock.getLockUntil());
        shedlockDTO.setLockedAt(shedlock.getLockedAt());
        shedlockDTO.setLockedBy(shedlock.getLockedBy());
        return shedlockDTO;
    }

    private Shedlock mapToEntity(final ShedlockDTO shedlockDTO, final Shedlock shedlock) {
        shedlock.setLockUntil(shedlockDTO.getLockUntil());
        shedlock.setLockedAt(shedlockDTO.getLockedAt());
        shedlock.setLockedBy(shedlockDTO.getLockedBy());
        return shedlock;
    }

}
