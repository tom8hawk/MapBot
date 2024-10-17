package ru.tom8hawk.mapbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tom8hawk.mapbot.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTelegramId(Long telegramId);
}