package net.graysenko.RTS.Persistent.Services;

import lombok.RequiredArgsConstructor;
import net.graysenko.RTS.Persistent.Entities.User;
import net.graysenko.RTS.Persistent.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
