# ==========================================
# STAGE 1: BUILD (Lingkungan Kompilasi)
# ==========================================
# Menggunakan Maven dengan JDK 17 di atas Alpine Linux (sangat ringan)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

# Set working directory di dalam container
WORKDIR /app

# Salin file POM terlebih dahulu untuk caching dependency (optimasi build)
COPY pom.xml .

# Unduh semua dependency secara offline (ini akan di-cache oleh Docker)
RUN mvn dependency:go-offline -B

# Salin seluruh source code
COPY src ./src

# Compile dan package aplikasi menjadi file JAR (tanpa menjalankan unit test untuk mempercepat)
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: PRODUCTION RUNTIME
# ==========================================
# Gunakan JRE saja (bukan JDK) karena kita hanya perlu menjalankan aplikasinya, bukan compile
# Alpine Linux meminimalisir attack surface karena tidak ada bash/utilitas linux lengkap
FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="Tim Blue Team NetraSphere"
LABEL description="Smart Waste System Backend Container"

# Set working directory
WORKDIR /app

# 1. SECURITY FEATURE: Non-Root User
# Sangat berbahaya jika Spring Boot dijalankan sebagai root di dalam container.
# Kita buat grup 'spring' dan user 'spring', dan jalankan aplikasi dengan user ini.
RUN addgroup -S spring && adduser -S spring -G spring

# 2. SECURITY FEATURE: Ownership & Permissions
# Buat folder untuk logs dan uploads (jika aplikasi menulis ke disk)
# dan berikan akses ke user 'spring'
RUN mkdir -p /app/logs /app/uploads && \
    chown -R spring:spring /app

# Pindah ke user non-root
USER spring:spring

# Salin JAR hasil kompilasi dari STAGE 1 ke STAGE 2
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Ekspos port yang digunakan oleh aplikasi (sesuai application.yml)
EXPOSE 8081

# Jalankan aplikasi
ENTRYPOINT ["java", "-jar", "app.jar"]
