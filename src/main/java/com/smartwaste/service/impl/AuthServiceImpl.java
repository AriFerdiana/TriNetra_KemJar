package com.smartwaste.service.impl;

import com.smartwaste.config.JwtUtil;
import com.smartwaste.dto.request.LoginRequest;
import com.smartwaste.dto.request.RegisterCitizenRequest;
import com.smartwaste.dto.response.AuthResponse;
import com.smartwaste.entity.Citizen;
import com.smartwaste.entity.GreenWallet;
import com.smartwaste.entity.User;
import com.smartwaste.exception.DuplicateEmailException;
import com.smartwaste.repository.CitizenRepository;
import com.smartwaste.repository.GreenWalletRepository;
import com.smartwaste.repository.UserRepository;
import com.smartwaste.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementasi service autentikasi.
 *
 * <p><b>OOP:</b> Implements {@link AuthService} interface. Menggunakan
 * {@code @Transactional} memastikan registrasi citizen + wallet dibuat atomically.</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final CitizenRepository citizenRepository;
    private final GreenWalletRepository greenWalletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           CitizenRepository citizenRepository,
                           GreenWalletRepository greenWalletRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.citizenRepository = citizenRepository;
        this.greenWalletRepository = greenWalletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterCitizenRequest request) {
        // 1. Validasi email unik
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        // 2. Buat Citizen baru (Inheritance: Citizen extends User extends BaseEntity)
        Citizen citizen = new Citizen(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getPhone(),
                request.getNik(),
                request.getAddress()
        );
        citizen.setRtRw(request.getRtRw());
        citizen.setKelurahan(request.getKelurahan());

        Citizen savedCitizen = citizenRepository.save(citizen);
        log.info("Citizen baru terdaftar: {}", savedCitizen.getEmail());

        // 3. Buat GreenWallet untuk citizen baru (Encapsulation: logika wallet di entity)
        GreenWallet wallet = new GreenWallet(savedCitizen);
        greenWalletRepository.save(wallet);
        log.info("GreenWallet dibuat untuk citizen: {}", savedCitizen.getId());

        // 4. Generate JWT token
        String token = jwtUtil.generateToken(savedCitizen);

        return AuthResponse.builder()
                .token(token)
                .userId(savedCitizen.getId())
                .name(savedCitizen.getName())
                .email(savedCitizen.getEmail())
                .role(savedCitizen.getRole())
                .expiresIn(jwtUtil.getJwtExpiration())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Spring Security menangani validasi email + password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Jika di sini, berarti autentikasi berhasil
        User user = userRepository.findActiveByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        String token = jwtUtil.generateToken(user);
        log.info("User login: {} ({})", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresIn(jwtUtil.getJwtExpiration())
                .build();
    }

    @Override
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Password lama salah");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password user {} berhasil diubah", email);
    }
}
