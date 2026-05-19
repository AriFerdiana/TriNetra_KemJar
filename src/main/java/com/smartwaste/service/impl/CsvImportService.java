package com.smartwaste.service.impl;

import com.opencsv.CSVReader;
import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.GreenWallet;
import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.repository.GreenWalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

@Service
public class CsvImportService {

    private final CitizenRepository citizenRepository;
    private final GreenWalletRepository greenWalletRepository;
    private final PasswordEncoder passwordEncoder;

    public CsvImportService(CitizenRepository citizenRepository,
                            GreenWalletRepository greenWalletRepository,
                            PasswordEncoder passwordEncoder) {
        this.citizenRepository = citizenRepository;
        this.greenWalletRepository = greenWalletRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String importCitizens(MultipartFile file) {
        if (file.isEmpty()) {
            return "File CSV kosong!";
        }

        int successCount = 0;
        int failCount = 0;

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> records = csvReader.readAll();
            
            // Assume first row is header: Nama, Email, NIK, NoHP, Alamat, Password
            if (records.size() <= 1) {
                return "File CSV tidak memiliki data yang valid.";
            }

            for (int i = 1; i < records.size(); i++) {
                String[] row = records.get(i);
                
                if (row.length < 6) {
                    failCount++;
                    continue;
                }

                String name = row[0].trim();
                String email = row[1].trim();
                String nik = row[2].trim();
                String phone = row[3].trim();
                String address = row[4].trim();
                String passwordRaw = row[5].trim();

                if (email.isEmpty() || name.isEmpty() || passwordRaw.isEmpty()) {
                    failCount++;
                    continue;
                }

                if (citizenRepository.existsByEmail(email)) {
                    failCount++;
                    continue;
                }

                Citizen citizen = new Citizen();
                citizen.setName(name);
                citizen.setEmail(email);
                citizen.setNik(nik);
                citizen.setPhone(phone);
                citizen.setAddress(address);
                citizen.setPassword(passwordEncoder.encode(passwordRaw));
                citizen.setActive(true);

                citizen = citizenRepository.save(citizen);

                GreenWallet wallet = new GreenWallet(citizen);
                greenWalletRepository.save(wallet);

                successCount++;
            }

        } catch (Exception e) {
            return "Terjadi kesalahan saat parsing CSV: " + e.getMessage();
        }

        return String.format("Berhasil mengimpor %d warga. Gagal: %d.", successCount, failCount);
    }
}
