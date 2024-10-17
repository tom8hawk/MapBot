package ru.tom8hawk.mapbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tom8hawk.mapbot.model.User;
import ru.tom8hawk.mapbot.repository.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    public User save(User newUser) {
        return userRepository.save(newUser);
    }
}
