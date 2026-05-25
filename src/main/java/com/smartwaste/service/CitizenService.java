package com.smartwaste.service;

import com.smartwaste.dto.response.CitizenProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface untuk manajemen warga (Citizen).
 */
public interface CitizenService {
    CitizenProfileResponse getMyProfile(String citizenEmail);
    CitizenProfileResponse getById(String citizenId);
    Page<CitizenProfileResponse> getAllCitizens(Pageable pageable);
    Page<CitizenProfileResponse> searchCitizens(String keyword, Boolean active, Pageable pageable);
    CitizenProfileResponse updateProfile(String citizenId, String name, String phone, String address);
    void deactivateCitizen(String citizenId);
    void toggleCitizenActive(String citizenId);
    long countActive();
    void importCitizensFromCsv(org.springframework.web.multipart.MultipartFile file);
    void resetPassword(String citizenId, String newPassword);

    CitizenProfileResponse createCitizen(String name, String email, String password, String nik, String phone, String address, String rtRw, String kelurahan);
    
    CitizenProfileResponse adminUpdateCitizen(String citizenId, String name, String email, String nik, String phone, String address, String rtRw, String kelurahan);
    
    void deleteCitizen(String citizenId);
}