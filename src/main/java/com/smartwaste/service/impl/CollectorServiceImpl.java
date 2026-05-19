package com.smartwaste.service.impl;

import com.smartwaste.dto.request.RegisterCollectorRequest;
import com.smartwaste.dto.response.CollectorResponse;
import com.smartwaste.entity.Collector;
import com.smartwaste.exception.DuplicateEmailException;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.CollectorRepository;
import com.smartwaste.repository.UserRepository;
import com.smartwaste.repository.WasteDepositRepository;
import com.smartwaste.service.CollectorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementasi CollectorService — Manajemen petugas pengumpul sampah.
 *
 * <p><b>OOP Concept — Polymorphism via Interface:</b>
 * Mengimplementasikan {@link CollectorService} dan meng-override semua method yang
 * didefinisikan di interface tersebut.</p>
 */
@Service
@Transactional(readOnly = true)
public class CollectorServiceImpl implements CollectorService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CollectorServiceImpl.class);

    private final CollectorRepository collectorRepository;
    private final UserRepository userRepository;
    private final WasteDepositRepository depositRepository;
    private final PasswordEncoder passwordEncoder;

    public CollectorServiceImpl(CollectorRepository collectorRepository,
                                UserRepository userRepository,
                                WasteDepositRepository depositRepository,
                                PasswordEncoder passwordEncoder) {
        this.collectorRepository = collectorRepository;
        this.userRepository = userRepository;
        this.depositRepository = depositRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public CollectorResponse registerCollector(RegisterCollectorRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        Collector collector = new Collector(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhone() != null ? request.getPhone() : "",
                request.getVehicleNumber() != null ? request.getVehicleNumber() : "-",
                request.getAssignedArea() != null ? request.getAssignedArea() : "Belum ditentukan"
        );

        Collector saved = collectorRepository.save(collector);
        log.info("Collector baru terdaftar: {}", saved.getEmail());
        return mapToResponse(saved);
    }

    @Override
    public Page<CollectorResponse> getAllCollectors(Pageable pageable) {
        return collectorRepository.findByActiveTrue(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<CollectorResponse> searchCollectors(String keyword, Boolean active, Pageable pageable) {
        return collectorRepository.searchCollectors(keyword, active, pageable).map(this::mapToResponse);
    }

    @Override
    public CollectorResponse getById(String id) {
        Collector collector = collectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", id));
        return mapToResponse(collector);
    }

    @Override
    @Transactional
    public CollectorResponse updateCollector(String id, String name, String phone,
                                              String vehicleNumber, String assignedArea) {
        Collector collector = collectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", id));
        if (name != null && !name.isBlank()) collector.setName(name);
        if (phone != null) collector.setPhone(phone);
        if (vehicleNumber != null) collector.setVehicleNumber(vehicleNumber);
        if (assignedArea != null) collector.setAssignedArea(assignedArea);
        return mapToResponse(collectorRepository.save(collector));
    }

    @Override
    @Transactional
    public void toggleActive(String id) {
        Collector collector = collectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", id));
        collector.setActive(!collector.isActive());
        collectorRepository.save(collector);
    }

    @Override
    @Transactional
    public CollectorResponse setAvailability(String collectorEmail, boolean available) {
        Collector collector = collectorRepository.findByEmail(collectorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "email", collectorEmail));
        collector.setAvailable(available);
        return mapToResponse(collectorRepository.save(collector));
    }

    @Override
    public long countActive() {
        return collectorRepository.countByActiveTrue();
    }

    @Override
    @Transactional
    public void resetPassword(String collectorId, String newPassword) {
        Collector collector = collectorRepository.findById(collectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector", "id", collectorId));
        collector.setPassword(passwordEncoder.encode(newPassword));
        collectorRepository.save(collector);
    }

    private CollectorResponse mapToResponse(Collector c) {
        long totalConfirmed = depositRepository.countByCollector(c);
        return CollectorResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .vehicleNumber(c.getVehicleNumber())
                .assignedArea(c.getAssignedArea())
                .available(c.isAvailable())
                .iotDevice(c.isIotDevice())
                .iotDeviceId(c.getIotDeviceId())
                .active(c.isActive())
                .totalConfirmed(totalConfirmed)
                .createdAt(c.getCreatedAt())
                .build();
    }
}
