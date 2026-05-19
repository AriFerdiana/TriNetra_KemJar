package com.smartwaste.service;

import com.smartwaste.entity.Faq;

import java.util.List;
import java.util.Optional;

public interface FaqService {
    List<Faq> getAllFaqs();
    List<Faq> getActiveFaqs();
    Optional<Faq> getFaqById(String id);
    Faq saveFaq(Faq faq);
    void deleteFaq(String id);
    void toggleActive(String id);
}
