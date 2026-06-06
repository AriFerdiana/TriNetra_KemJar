package com.smartwaste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point aplikasi Smart Community System: Sistem Pengelolaan Sampah.
 *
 * <p>Arsitektur: Controller → Service Interface & Impl → Repository → Entity → DTO</p>
 * <p>Konsep OOP: Encapsulation, Inheritance, Polymorphism, Interface, Abstract Class,
 * Custom Exception Handling</p>
 *
 * @author Trinetra — Tugas Besar PBO
 * @version 1.0.0
 */
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class SmartWasteApplication {

    // Trigger restart for seeding
    public static void main(String[] args) {
        SpringApplication.run(SmartWasteApplication.class, args);
    }
}




