package com.smartwaste.repository;

import com.smartwaste.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, String> {
    List<Faq> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<Faq> findAllByOrderByDisplayOrderAsc();
}
