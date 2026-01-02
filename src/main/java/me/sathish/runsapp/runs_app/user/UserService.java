package me.sathish.runsapp.runs_app.user;

import java.util.List;
import java.util.Map;


public interface UserService {

    List<UserDTO> findAll();

    UserDTO get(Long id);

    Long create(UserDTO userDTO);

    void update(Long id, UserDTO userDTO);

    void delete(Long id);

    Map<Long, String> getUserValues();

}
