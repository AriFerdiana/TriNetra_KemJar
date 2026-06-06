package com.smartwaste.controller;

import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.entity.Citizen;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class DebugController {

    private final CitizenRepository citizenRepository;

    private final com.smartwaste.service.CitizenService citizenService;

    public DebugController(CitizenRepository citizenRepository, com.smartwaste.service.CitizenService citizenService) {
        this.citizenRepository = citizenRepository;
        this.citizenService = citizenService;
    }

    @GetMapping("/api/v1/auth/debug-citizens")
    public String debugCitizens() {
        try {
            Page<Citizen> repoSearch = citizenRepository.searchCitizens("", null, PageRequest.of(0, 10));
            Page<Citizen> repoAll = citizenRepository.findAll(PageRequest.of(0, 10));
            Page<com.smartwaste.dto.response.CitizenProfileResponse> serviceAll = citizenService.searchCitizens("", null, PageRequest.of(0, 10));
            
            return "Repo Search: " + repoSearch.getTotalElements() + 
                   " | Repo All: " + repoAll.getTotalElements() + 
                   " | Service All: " + serviceAll.getTotalElements();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
