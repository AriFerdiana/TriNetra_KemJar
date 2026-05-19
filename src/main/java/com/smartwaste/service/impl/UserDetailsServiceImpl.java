package com.smartwaste.service.impl;

import com.smartwaste.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementasi {@link UserDetailsService} — menghubungkan Spring Security
 * dengan database user kita.
 *
 * <p>Dipanggil oleh Spring Security saat memvalidasi token JWT
 * (di {@code JwtAuthFilter}) dan saat memproses form login.</p>
 *
 * <p>Karena {@code User} entity sudah implements {@code UserDetails},
 * kita cukup return hasil query dari database tanpa wrapper tambahan.</p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final com.smartwaste.security.LoginAttemptService loginAttemptService;

    public UserDetailsServiceImpl(UserRepository userRepository,
                                  com.smartwaste.security.LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (loginAttemptService.isBlocked(email)) {
            throw new org.springframework.security.authentication.LockedException("Akun diblokir sementara karena terlalu banyak percobaan login yang gagal.");
        }

        return userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User tidak ditemukan dengan email: " + email));
    }
}
