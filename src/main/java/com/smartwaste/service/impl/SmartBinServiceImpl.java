package com.smartwaste.service.impl;

import com.smartwaste.entity.SmartBin;
import com.smartwaste.repository.SmartBinRepository;
import com.smartwaste.service.SmartBinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SmartBinServiceImpl implements SmartBinService {

    private final SmartBinRepository repository;

    public SmartBinServiceImpl(SmartBinRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmartBin> getAllBins() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public void updateCapacity(String deviceId, double capacity) {
        repository.findByDeviceId(deviceId).ifPresent(bin -> {
            bin.setFillLevel((int) capacity);
            if (capacity >= 90) {
                bin.setStatus("Penuh");
                bin.setColor("#ef4444"); // Red
            } else if (capacity >= 70) {
                bin.setStatus("Hampir Penuh");
                bin.setColor("#f59e0b"); // Amber
            } else {
                bin.setStatus("Aktif");
                bin.setColor("#10b981"); // Emerald
            }
            repository.save(bin);
        });
    }

    @Override
    @Transactional
    public void toggleStatus(String id) {
        repository.findById(id).ifPresent(bin -> {
            if ("Offline".equals(bin.getStatus())) {
                bin.setStatus("Aktif");
            } else {
                bin.setStatus("Offline");
                bin.setColor("#6b7280"); // Gray
            }
            repository.save(bin);
        });
    }
}
