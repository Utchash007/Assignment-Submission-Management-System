package com.asms.springasms.repository;

import com.asms.springasms.entity.User;
import com.asms.springasms.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    List<User> findAllByOrderByFullNameAsc();

    List<User> findByRoleOrderByFullNameAsc(UserRole role);
}
