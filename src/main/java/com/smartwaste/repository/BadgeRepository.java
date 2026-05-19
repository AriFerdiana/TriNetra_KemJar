package com.smartwaste.repository;

import com.smartwaste.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, String> {
    Optional<Badge> findByName(String name);
}
