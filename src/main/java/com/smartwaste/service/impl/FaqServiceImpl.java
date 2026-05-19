package com.smartwaste.service.impl;

import com.smartwaste.entity.Faq;
import com.smartwaste.repository.FaqRepository;
import com.smartwaste.service.FaqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;

    @Autowired
    public FaqServiceImpl(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Override
    public List<Faq> getAllFaqs() {
        return faqRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public List<Faq> getActiveFaqs() {
        return faqRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public Optional<Faq> getFaqById(String id) {
        return faqRepository.findById(id);
    }

    @Override
    public Faq saveFaq(Faq faq) {
        if (faq.getDisplayOrder() == null) {
            faq.setDisplayOrder(0);
        }
        return faqRepository.save(faq);
    }

    @Override
    public void deleteFaq(String id) {
        faqRepository.deleteById(id);
    }

    @Override
    public void toggleActive(String id) {
        Optional<Faq> faqOpt = faqRepository.findById(id);
        if (faqOpt.isPresent()) {
            Faq faq = faqOpt.get();
            faq.setActive(!faq.isActive());
            faqRepository.save(faq);
        }
    }
}
