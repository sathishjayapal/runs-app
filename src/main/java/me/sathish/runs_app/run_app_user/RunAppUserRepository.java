package me.sathish.runs_app.run_app_user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface RunAppUserRepository extends JpaRepository<RunAppUser, Long> {

    RunAppUser findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM RunAppUser u LEFT JOIN FETCH u.roles WHERE LOWER(u.email) = LOWER(:email)")
    RunAppUser findByEmailIgnoreCaseWithRoles(@Param("email") String email);

    Page<RunAppUser> findAllById(Long id, Pageable pageable);

}
