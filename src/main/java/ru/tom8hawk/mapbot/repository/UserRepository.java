package ru.tom8hawk.mapbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tom8hawk.mapbot.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByTelegramId(Long telegramId);
}