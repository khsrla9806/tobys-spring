package org.example.user.service;

import org.example.user.domain.User;

public interface UserService {
    void add(User user);

    void upgradeLevels();
}
