package me.sathish.runs_app.run_app_user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RunAppUserRepository extends JpaRepository<RunAppUser, Long> {

    RunAppUser findByEmailIgnoreCase(String email);

    Page<RunAppUser> findAllById(Long id, Pageable pageable);

}
