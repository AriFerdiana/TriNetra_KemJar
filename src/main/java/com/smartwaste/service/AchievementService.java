package com.smartwaste.service;

import com.smartwaste.entity.Badge;
import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.CitizenBadge;
import com.smartwaste.repository.BadgeRepository;
import com.smartwaste.repository.CitizenBadgeRepository;
import com.smartwaste.repository.WasteDepositRepository;
import com.smartwaste.entity.enums.DepositStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final BadgeRepository badgeRepository;
    private final CitizenBadgeRepository citizenBadgeRepository;
    private final WasteDepositRepository depositRepository;

    @Transactional
    public void checkAndAwardBadges(Citizen citizen) {
        List<Badge> allBadges = badgeRepository.findAll();
        
        long totalDeposits = depositRepository.countByCitizenAndStatus(citizen, DepositStatus.CONFIRMED);
        double totalWeight = depositRepository.sumWeightByCitizen(citizen);
        
        for (Badge badge : allBadges) {
            if (!citizenBadgeRepository.existsByCitizenAndBadge_Id(citizen, badge.getId())) {
                boolean earned = false;
                
                switch (badge.getRequirementType()) {
                    case "TOTAL_DEPOSITS":
                        if (totalDeposits >= badge.getThreshold()) earned = true;
                        break;
                    case "TOTAL_WEIGHT":
                        if (totalWeight >= badge.getThreshold()) earned = true;
                        break;
                }
                
                if (earned) {
                    CitizenBadge citizenBadge = new CitizenBadge();
                    citizenBadge.setCitizen(citizen);
                    citizenBadge.setBadge(badge);
                    citizenBadgeRepository.save(citizenBadge);
                }
            }
        }
    }

    public List<CitizenBadge> getCitizenBadges(Citizen citizen) {
        return citizenBadgeRepository.findByCitizen(citizen);
    }
}
