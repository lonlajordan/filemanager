package com.filemanager.repositories;

import com.filemanager.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUsername(String username);
    List<User> findAllByOrderByLastLoginDesc();
}
