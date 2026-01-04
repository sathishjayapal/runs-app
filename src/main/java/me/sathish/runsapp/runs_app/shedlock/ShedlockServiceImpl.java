package me.sathish.runsapp.runs_app.shedlock;

import me.sathish.runsapp.runs_app.util.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class ShedlockServiceImpl implements ShedlockService {

    private final ShedlockRepository shedlockRepository;

    public ShedlockServiceImpl(final ShedlockRepository shedlockRepository) {
        this.shedlockRepository = shedlockRepository;
    }

    @Override
    public Page<ShedlockDTO> findAll(final String filter, final Pageable pageable) {
        Page<Shedlock> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = shedlockRepository.findAllByName(longFilter, pageable);
        } else {
            page = shedlockRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(shedlock -> mapToDTO(shedlock, new ShedlockDTO()))
                .toList(),
                pageable, page.getTotalElements());
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
