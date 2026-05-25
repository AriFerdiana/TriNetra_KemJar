package com.smartwaste.service.impl;

import com.smartwaste.dto.response.CitizenProfileResponse;
import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.GreenWallet;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.repository.GreenWalletRepository;
import com.smartwaste.repository.WasteDepositRepository;
import com.smartwaste.service.CitizenService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementasi service manajemen warga.
 */
@Service
@Transactional(readOnly = true)
public class CitizenServiceImpl implements CitizenService {

    private final CitizenRepository citizenRepository;
    private final GreenWalletRepository greenWalletRepository;
    private final WasteDepositRepository wasteDepositRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public CitizenServiceImpl(CitizenRepository citizenRepository,
                              GreenWalletRepository greenWalletRepository,
                              WasteDepositRepository wasteDepositRepository,
                              org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.citizenRepository = citizenRepository;
        this.greenWalletRepository = greenWalletRepository;
        this.wasteDepositRepository = wasteDepositRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CitizenProfileResponse getMyProfile(String citizenEmail) {
        Citizen citizen = citizenRepository.findByEmail(citizenEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "email", citizenEmail));
        return mapToResponse(citizen);
    }

    @Override
    public CitizenProfileResponse getById(String citizenId) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "id", citizenId));
        return mapToResponse(citizen);
    }

    @Override
    public Page<CitizenProfileResponse> getAllCitizens(Pageable pageable) {
        return citizenRepository.findByActiveTrue(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<CitizenProfileResponse> searchCitizens(String keyword, Boolean active, Pageable pageable) {
        return citizenRepository.searchCitizens(keyword, active, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CitizenProfileResponse updateProfile(String citizenId, String name, String phone, String address) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "id", citizenId));
        if (name != null && !name.isBlank()) citizen.setName(name);
        if (phone != null) citizen.setPhone(phone);
        if (address != null) citizen.setAddress(address);
        return mapToResponse(citizenRepository.save(citizen));
    }

    @Override
    @Transactional
    public void deactivateCitizen(String citizenId) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "id", citizenId));
        citizen.setActive(false);
        citizenRepository.save(citizen);
    }

    @Override
    @Transactional
    public void toggleCitizenActive(String citizenId) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "id", citizenId));
        citizen.setActive(!citizen.isActive());
        citizenRepository.save(citizen);
    }

    @Override
    public long countActive() {
        return citizenRepository.countByActiveTrue();
    }

    @Override
    @Transactional
    public void importCitizensFromCsv(org.springframework.web.multipart.MultipartFile file) {
        try (java.io.InputStreamReader reader = new java.io.InputStreamReader(file.getInputStream());
             com.opencsv.CSVReader csvReader = new com.opencsv.CSVReaderBuilder(reader).withSkipLines(1).build()) {
            
            String[] line;
            
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 3) continue;
                
                String name = line[0];
                String email = line[1];
                String phone = line[2];
                
                if (citizenRepository.existsByEmail(email)) continue;
                
                Citizen citizen = new Citizen();
                citizen.setName(name);
                citizen.setEmail(email);
                citizen.setPhone(phone);
                citizen.setPassword(passwordEncoder.encode("warga123")); // Default password
                citizen.setActive(true);
                
                Citizen saved = citizenRepository.save(citizen);
                
                // Initialize wallet
                GreenWallet wallet = new GreenWallet(saved);
                greenWalletRepository.save(wallet);
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal mengimpor data warga: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void resetPassword(String citizenId, String newPassword) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "id", citizenId));
        citizen.setPassword(passwordEncoder.encode(newPassword));
        citizenRepository.save(citizen);
    }


    @Override
    @Transactional
    public CitizenProfileResponse createCitizen(String name, String email, String password, String nik, String phone, String address, String rtRw, String kelurahan) {
        if (citizenRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email sudah terdaftar!");
        }
        
        Citizen citizen = new Citizen();
        citizen.setName(name);
        citizen.setEmail(email);
        citizen.setPassword(passwordEncoder.encode(password));
        citizen.setNik(nik != null && !nik.trim().isEmpty() ? nik : null);
        citizen.setPhone(phone != null && !phone.trim().isEmpty() ? phone : null);
        citizen.setAddress(address);
        citizen.setRtRw(rtRw);
        citizen.setKelurahan(kelurahan);
        citizen.setActive(true);
        
        Citizen saved = citizenRepository.save(citizen);
        
        GreenWallet wallet = new GreenWallet(saved);
        greenWalletRepository.save(wallet);
        
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CitizenProfileResponse adminUpdateCitizen(String citizenId, String name, String email, String nik, String phone, String address, String rtRw, String kelurahan) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "id", citizenId));
                
        // Check if email changed and is available
        if (email != null && !email.isBlank() && !email.equals(citizen.getEmail())) {
            if (citizenRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email sudah terdaftar!");
            }
            citizen.setEmail(email);
        }
        
        if (name != null && !name.isBlank()) citizen.setName(name);
        if (nik != null) citizen.setNik(nik);
        if (phone != null) citizen.setPhone(phone);
        if (address != null) citizen.setAddress(address);
        if (rtRw != null) citizen.setRtRw(rtRw);
        if (kelurahan != null) citizen.setKelurahan(kelurahan);
        
        return mapToResponse(citizenRepository.save(citizen));
    }

    @Override
    @Transactional
    public void deleteCitizen(String citizenId) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen", "id", citizenId));
        citizenRepository.delete(citizen);
    }

    /** Mapper: Citizen entity → CitizenProfileResponse DTO (Encapsulation) */
    private CitizenProfileResponse mapToResponse(Citizen citizen) {
        GreenWallet wallet = greenWalletRepository.findByCitizen(citizen).orElse(null);
        double totalWeight = wasteDepositRepository.sumWeightByCitizen(citizen);
        long totalDeposits = wasteDepositRepository.countByCitizenAndStatus(citizen, com.smartwaste.entity.enums.DepositStatus.CONFIRMED);

        return CitizenProfileResponse.builder()
                .id(citizen.getId())
                .name(citizen.getName())
                .email(citizen.getEmail())
                .phone(citizen.getPhone())
                .nik(citizen.getNik())
                .address(citizen.getAddress())
                .rtRw(citizen.getRtRw())
                .kelurahan(citizen.getKelurahan())
                .active(citizen.isActive())
                .createdAt(citizen.getCreatedAt())
                .totalPoints(wallet != null ? wallet.getTotalPoints() : 0)
                .availablePoints(wallet != null ? wallet.getAvailablePoints() : 0)
                .redeemedPoints(wallet != null ? wallet.getRedeemedPoints() : 0)
                .totalDeposits(totalDeposits)
                .totalWeightKg(totalWeight)
                .build();
    }
}
