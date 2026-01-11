package me.sathish.runs_app.run_app_user;

import java.util.Map;
import me.sathish.runs_app.events.BeforeDeleteRunAppUser;
import me.sathish.runs_app.util.CustomCollectors;
import me.sathish.runs_app.util.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class RunAppUserServiceImpl implements RunAppUserService {

    private final RunAppUserRepository runAppUserRepository;
    private final ApplicationEventPublisher publisher;
    private final PasswordEncoder passwordEncoder;

    public RunAppUserServiceImpl(final RunAppUserRepository runAppUserRepository,
            final ApplicationEventPublisher publisher, final PasswordEncoder passwordEncoder) {
        this.runAppUserRepository = runAppUserRepository;
        this.publisher = publisher;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Page<RunAppUserDTO> findAll(final String filter, final Pageable pageable) {
        Page<RunAppUser> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = runAppUserRepository.findAllById(longFilter, pageable);
        } else {
            page = runAppUserRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(runAppUser -> mapToDTO(runAppUser, new RunAppUserDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    @Override
    public RunAppUserDTO get(final Long id) {
        return runAppUserRepository.findById(id)
                .map(runAppUser -> mapToDTO(runAppUser, new RunAppUserDTO()))
                .orElseThrow(NotFoundException::new);
    }

    @Override
    public Long create(final RunAppUserDTO runAppUserDTO) {
        final RunAppUser runAppUser = new RunAppUser();
        mapToEntity(runAppUserDTO, runAppUser);
        return runAppUserRepository.save(runAppUser).getId();
    }

    @Override
    public void update(final Long id, final RunAppUserDTO runAppUserDTO) {
        final RunAppUser runAppUser = runAppUserRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        mapToEntity(runAppUserDTO, runAppUser);
        runAppUserRepository.save(runAppUser);
    }

    @Override
    public void delete(final Long id) {
        final RunAppUser runAppUser = runAppUserRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        publisher.publishEvent(new BeforeDeleteRunAppUser(id));
        runAppUserRepository.delete(runAppUser);
    }

    private RunAppUserDTO mapToDTO(final RunAppUser runAppUser, final RunAppUserDTO runAppUserDTO) {
        runAppUserDTO.setId(runAppUser.getId());
        runAppUserDTO.setEmail(runAppUser.getEmail());
        runAppUserDTO.setName(runAppUser.getName());
        runAppUserDTO.setRole(runAppUser.getRole());
        return runAppUserDTO;
    }

    private RunAppUser mapToEntity(final RunAppUserDTO runAppUserDTO, final RunAppUser runAppUser) {
        runAppUser.setEmail(runAppUserDTO.getEmail());
        runAppUser.setPassword(passwordEncoder.encode(runAppUserDTO.getPassword()));
        runAppUser.setName(runAppUserDTO.getName());
        runAppUser.setRole(runAppUserDTO.getRole());
        return runAppUser;
    }

    @Override
    public Map<Long, String> getRunAppUserValues() {
        return runAppUserRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(RunAppUser::getId, RunAppUser::getEmail));
    }

}
