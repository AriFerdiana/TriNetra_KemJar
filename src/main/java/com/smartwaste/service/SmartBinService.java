package com.smartwaste.service;

import com.smartwaste.entity.SmartBin;
import java.util.List;

public interface SmartBinService {
    List<SmartBin> getAllBins();
    void updateCapacity(String deviceId, double capacity);
    void toggleStatus(String id);
}
