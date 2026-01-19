package me.sathish.runs_app.shedlock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
public class ShedlockViewService {

    private final ShedlockViewRepository shedlockViewRepository;

    public ShedlockViewService(final ShedlockViewRepository shedlockViewRepository) {
        this.shedlockViewRepository = shedlockViewRepository;
    }

    public Page<ShedlockViewDTO> findAll(final Pageable pageable) {
        final Page<ShedlockView> shedlocks = shedlockViewRepository.findAll(pageable);
        return shedlocks.map(shedlock -> mapToDTO(shedlock, new ShedlockViewDTO()));
    }

    public ShedlockViewDTO get(final String name) {
        return shedlockViewRepository.findById(name)
                .map(shedlock -> mapToDTO(shedlock, new ShedlockViewDTO()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ShedlockViewDTO mapToDTO(final ShedlockView shedlock,
                                     final ShedlockViewDTO shedlockViewDTO) {
        shedlockViewDTO.setName(shedlock.getName());
        shedlockViewDTO.setLockUntil(shedlock.getLockUntil());
        shedlockViewDTO.setLockedAt(shedlock.getLockedAt());
        shedlockViewDTO.setLockedBy(shedlock.getLockedBy());
        return shedlockViewDTO;
    }

}
