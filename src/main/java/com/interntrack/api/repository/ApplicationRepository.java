package com.interntrack.api.repository;

import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByOwner(User owner);
    Optional<Application> findByIdAndOwner(Long id, User owner);
    void deleteByOwner(User owner);
}