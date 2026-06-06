package com.smartwaste.debug;

import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.entity.Citizen;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;

@Component
public class DebugCitizenSearch implements CommandLineRunner {

    private final CitizenRepository citizenRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("SECURITY_AUDIT");

    public DebugCitizenSearch(CitizenRepository citizenRepository) {
        this.citizenRepository = citizenRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.warn("DEBUG_CITIZEN_SEARCH: Starting search test...");
        try {
            Page<Citizen> result = citizenRepository.searchCitizens("", null, PageRequest.of(0, 10));
            log.warn("DEBUG_CITIZEN_SEARCH: Found " + result.getTotalElements() + " elements.");
            for (Citizen c : result.getContent()) {
                log.warn("DEBUG_CITIZEN_SEARCH: Citizen - " + c.getName());
            }
        } catch (Exception e) {
            log.error("DEBUG_CITIZEN_SEARCH: Exception occurred:", e);
        }
    }
}
