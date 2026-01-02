package me.sathish.runsapp.runs_app.user;

import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmailIgnoreCase(String email);

}
