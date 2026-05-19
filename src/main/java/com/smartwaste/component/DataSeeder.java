package com.smartwaste.component;

import com.smartwaste.entity.SmartBin;
import com.smartwaste.repository.SmartBinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final SmartBinRepository smartBinRepository;

    @Override
    public void run(String... args) throws Exception {
        if (smartBinRepository.count() < 30) {
            smartBinRepository.deleteAll(); // Hapus yang lama agar bersih
            
            // Seed 30 Smart Bins randomly around Itenas Campus
            // Itenas coordinate: -6.8975, 107.6350
            java.util.Random random = new java.util.Random();
            double centerLat = -6.8975;
            double centerLon = 107.6350;
            
            for (int i = 1; i <= 30; i++) {
                // Randomize position within ~1km radius (approx 0.01 degrees)
                double latOffset = (random.nextDouble() - 0.5) * 0.02;
                double lonOffset = (random.nextDouble() - 0.5) * 0.02;
                int level = random.nextInt(101); // 0-100%
                String status = level > 90 ? "Penuh" : (level > 70 ? "Peringatan" : "Aktif");
                
                String binId = String.format("SB-%03d", i);
                String name = "Smart Bin Itenas Zone " + (char)('A' + random.nextInt(26)) + i;
                
                createBin(binId, name, centerLat + latOffset, centerLon + lonOffset, level, status);
            }
        }
    }

    private void createBin(String id, String name, double lat, double lon, int level, String status) {
        SmartBin bin = new SmartBin();
        bin.setDeviceId(id);
        bin.setName(name);
        bin.setLatitude(lat);
        bin.setLongitude(lon);
        bin.setFillLevel(level);
        bin.setStatus(status);
        smartBinRepository.save(bin);
    }
}
