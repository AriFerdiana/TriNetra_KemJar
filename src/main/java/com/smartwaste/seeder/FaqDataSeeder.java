package com.smartwaste.seeder;

// Re-index trigger

import com.smartwaste.entity.Faq;
import com.smartwaste.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FaqDataSeeder implements CommandLineRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FaqDataSeeder.class);

    private final FaqRepository faqRepository;

    @Override
    public void run(String... args) throws Exception {
        if (faqRepository.count() == 0) {
            log.info(">>> SEEDER DEBUG: Seeding initial FAQs...");

            List<Faq> faqs = Arrays.asList(
                Faq.builder()
                    .question("Apa itu NetraSphere?")
                    .answer("NetraSphere adalah platform manajemen sampah cerdas (Smart Waste System) yang memfasilitasi penjemputan sampah terpilah dari rumah Anda menuju ke bank sampah atau tempat pengolahan.")
                    .displayOrder(1)
                    .isActive(true)
                    .build(),
                Faq.builder()
                    .question("Bagaimana cara mendapatkan Green Points?")
                    .answer("Anda akan mendapatkan poin setiap kali menyetorkan sampah yang sudah dipilah dengan benar. Semakin berat dan semakin berharga kategori sampahnya (seperti elektronik/B3), semakin besar poin yang Anda dapatkan.")
                    .displayOrder(2)
                    .isActive(true)
                    .build(),
                Faq.builder()
                    .question("Apa yang bisa saya lakukan dengan Green Points?")
                    .answer("Green Points dapat ditukarkan dengan berbagai hadiah menarik di Katalog Reward (Tukar Poin), mulai dari kebutuhan pokok (beras, minyak) hingga berdonasi bibit pohon atau pakan hewan jalanan.")
                    .displayOrder(3)
                    .isActive(true)
                    .build(),
                Faq.builder()
                    .question("Bolehkah membuang kotak pizza yang berminyak?")
                    .answer("Tidak! Kotak pizza berminyak tidak bisa didaur ulang karena minyak merusak serat kertas. Masukkan ke sampah organik/residu, atau potong bagian bersihnya saja yang ke anorganik.")
                    .displayOrder(4)
                    .isActive(true)
                    .build(),
                Faq.builder()
                    .question("Apakah tisu basah termasuk sampah organik?")
                    .answer("Tidak. Tisu basah mengandung plastik (polypropylene) yang tidak terurai. Ini termasuk sampah residu yang tidak bisa didaur ulang. Jangan gunakan berlebihan!")
                    .displayOrder(5)
                    .isActive(true)
                    .build(),
                Faq.builder()
                    .question("Bagaimana cara membuang minyak goreng bekas?")
                    .answer("Jangan tuang ke wastafel atau selokan! Bekukan dalam wadah tertutup, lalu buang ke tempat pengumpulan minyak jelantah, atau hubungi petugas NetraSphere untuk penanganan khusus.")
                    .displayOrder(6)
                    .isActive(true)
                    .build(),
                Faq.builder()
                    .question("Apakah styrofoam bisa didaur ulang?")
                    .answer("Secara teknis bisa, tapi sangat jarang karena biayanya mahal. Di Indonesia, styrofoam umumnya masuk sampah residu/B3. Sebaiknya hindari penggunaan styrofoam sama sekali.")
                    .displayOrder(7)
                    .isActive(true)
                    .build(),
                Faq.builder()
                    .question("Bolehkah buang baju bekas ke tempat sampah biasa?")
                    .answer("Jangan! Baju bekas yang masih layak pakai sebaiknya didonasikan ke yayasan sosial atau bank pakaian. Jika sudah tidak layak, kumpulkan ke bank daur ulang tekstil khusus.")
                    .displayOrder(8)
                    .isActive(true)
                    .build(),
                Faq.builder()
                    .question("Mengapa harus memisahkan tutup botol plastik?")
                    .answer("Tutup botol seringkali terbuat dari jenis plastik yang berbeda (PP) dari botolnya (PET). Memisahkan keduanya membantu proses penggilingan di pabrik daur ulang.")
                    .displayOrder(9)
                    .isActive(true)
                    .build(),
                Faq.builder()
                    .question("Kapan jadwal penjemputan armada?")
                    .answer("Armada kami beroperasi hari Senin - Sabtu. Ada shift pagi (07:00 - 09:00 WIB) dan shift sore (15:00 - 17:00 WIB). Pastikan Anda telah menekan tombol 'Setor Sekarang' sebelum jam tersebut.")
                    .displayOrder(10)
                    .isActive(true)
                    .build()
            );

            faqRepository.saveAll(faqs);
            log.info("✅ 10 FAQ awal berhasil ditambahkan ke database.");
        }
    }
}
