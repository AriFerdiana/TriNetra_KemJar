package com.smartwaste.seeder;

import com.smartwaste.entity.Article;
import com.smartwaste.entity.enums.ArticleType;
import com.smartwaste.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ArticleDataSeeder implements CommandLineRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ArticleDataSeeder.class);

    private final ArticleRepository articleRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) throws Exception {
        log.info(">>>> [SEEDER] Starting Article Data Seeding...");
        
        try {
            articleRepository.deleteAllInBatch();
            log.info(">>>> [SEEDER] Old articles cleared in batch.");

            List<Article> articles = Arrays.asList(
                Article.builder()
                    .title("Hari Peduli Sampah Nasional 2024: Atasi Polusi Plastik")
                    .content("Jakarta - Peringatan Hari Peduli Sampah Nasional (HPSN) 2024 mengusung tema 'Atasi Sampah Plastik dengan Cara Produktif'. Pemerintah mengajak seluruh elemen masyarakat untuk mulai memilah sampah dari rumah demi masa depan yang lebih hijau.")
                    .type(ArticleType.NEWS)
                    .externalImageUrl("https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?auto=format&fit=crop&w=800")
                    .author("Admin Netra")
                    .published(true)
                    .build(),

                Article.builder()
                    .title("Workshop DIY: Mengubah Limbah Tekstil Menjadi Fashion")
                    .content("Bandung - Belajar cara mengolah pakaian bekas dan kain perca menjadi tas, pouch, dan aksesoris menarik. Acara ini bertujuan untuk mengurangi limbah fashion dan mendorong gaya hidup slow fashion di kalangan anak muda.")
                    .type(ArticleType.EVENT)
                    .externalImageUrl("https://images.unsplash.com/photo-1558584673-c834fb1bb370?auto=format&fit=crop&w=800")
                    .author("Admin Netra")
                    .published(true)
                    .build(),

                Article.builder()
                    .title("Jakarta Larang Kantong Plastik di Pasar Rakyat")
                    .content("Jakarta - Pemerintah Provinsi DKI Jakarta memperluas larangan penggunaan kantong plastik sekali pakai hingga ke pasar tradisional. Kebijakan ini diharapkan dapat mengurangi timbulan sampah plastik secara signifikan di ibu kota.")
                    .type(ArticleType.NEWS)
                    .externalImageUrl("https://th.bing.com/th/id/OIP.sGqitDwpPJUJa50sqpps1gHaE8?w=251&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3")
                    .author("Admin Netra")
                    .published(true)
                    .build(),

                Article.builder()
                    .title("Aksi Bersih Pantai di Bali: Relawan Kumpulkan 5 Ton Sampah")
                    .content("Bali - Gerakan massa yang melibatkan ribuan warga lokal dan wisatawan berhasil membersihkan pesisir pantai Kuta dari kiriman sampah plastik. Aksi ini menunjukkan kepedulian kolektif terhadap ekosistem laut Indonesia.")
                    .type(ArticleType.EVENT)
                    .externalImageUrl("https://th.bing.com/th/id/OIP.yf20ybCifuYXdlGjT1tvJwHaEK?w=298&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3")
                    .author("Admin Netra")
                    .published(true)
                    .build(),

                Article.builder()
                    .title("Mengenal Maggot BSF: Solusi Cerdas Olah Sampah Organik")
                    .content("Edukasi - Larva Black Soldier Fly (BSF) atau maggot mampu mengonsumsi sampah organik hingga 2 kali berat tubuhnya. Teknologi ini mulai banyak diterapkan di tingkat rumah tangga maupun komunitas.")
                    .type(ArticleType.EDUCATION)
                    .externalImageUrl("https://images.unsplash.com/photo-1591955506264-3f5a6834570a?auto=format&fit=crop&w=800")
                    .author("Admin Netra")
                    .published(true)
                    .build(),

                Article.builder()
                    .title("NetraSphere Hadirkan AI untuk Identifikasi Kategori Sampah")
                    .content("Teknologi - Integrasi AI terbaru pada platform NetraSphere memungkinkan warga bertanya langsung mengenai cara pemilahan sampah yang benar hanya dengan mengunggah foto sampah tersebut.")
                    .type(ArticleType.NEWS)
                    .externalImageUrl("https://images.unsplash.com/photo-1620712943543-bcc4638d9980?auto=format&fit=crop&w=800")
                    .author("Admin Netra")
                    .published(true)
                    .build(),

                Article.builder()
                    .title("Penanaman 10.000 Mangrove di PIK untuk Cegah Abrasi")
                    .content("Jakarta Utara - Kolaborasi komunitas pecinta lingkungan berhasil menanam 10.000 bibit mangrove di kawasan pesisir Pantai Indah Kapuk. Ini adalah langkah nyata dalam menjaga keseimbangan ekosistem pesisir.")
                    .type(ArticleType.EVENT)
                    .externalImageUrl("data:image/webp;base64,UklGRlRkAABXRUJQVlA4IEhkAABQIAGdASraAdMAPpU6lkgloyIhMxi8OLASiUfa3nI1/6uaXOFe3Le9dc2GeAYnhFrr+uPtIlP5H8zAp3pr2fqW3IbNy/Rf3r/c+Bv5l9U/tf7/7TX49hj+D/sfMX7SJ3v7DvR+UGoR7f81f8js8do/3f7aewR7r/gvPy+y8z/3L/QewD5if9bwYfxX/H/c34Av6p/qPR70JvsP+//cD4GfLs/////+In7u////2/E+qyg2Po9LNlMTYnw7IKC75oigLcS4f5g/mEw/haW9j1Cla+IYEk9bC28HwdpCIGidcSj3aYSskK131UUYYm7UzkInUEmfLE6oNwYsHZTD8+ISL303fVrWbo94BbXUXU7wblAaMcCW025Id5E9/0RR9DCsthlQFbxMsX770iUd66BrXoHAVT6ASLbfH3EiYRZq8lRwyah5z4gNj+Khlmk7RdwbnXod5+4+1sdYRKsK7wvo3Mw2WyKmIEX0/+Ufp/urnoD74bNVoSfxJanPycdnfbD/wjjXzyWSf2+splt0CqWj5adZz/uE7a9WJ08UEfPLf8OS17HfzfiHelMaif3KsvJ9dhUTZpcnV2fV+VTkIAI5B5vEFfVG9mhnv7l8I8scdaQoEUb3O+8ru7X9s2HSeJsv4rvtf//y2dA8OE")
                    .author("Admin Netra")
                    .published(true)
                    .build(),

                Article.builder()
                    .title("Bahaya Limbah B3: Jangan Buang Baterai Sembarangan!")
                    .content("Penting - Baterai dan lampu neon termasuk kategori limbah B3. Jika dibuang sembarangan, zat kimia di dalamnya dapat mencemari air tanah dan membahayakan kesehatan masyarakat.")
                    .type(ArticleType.EDUCATION)
                    .externalImageUrl("https://images.unsplash.com/photo-1591955506264-3f5a6834570a?auto=format&fit=crop&w=800")
                    .author("Admin Netra")
                    .published(true)
                    .build(),

                Article.builder()
                    .title("Lomba Desain Produk Upcycling Antar Sekolah se-Jawa Barat")
                    .content("Kompetisi - Panggilan bagi inovator muda! Tunjukkan kreativitasmu dalam mengubah barang bekas menjadi produk bernilai guna tinggi dan menangkan hadiah jutaan rupiah.")
                    .type(ArticleType.EVENT)
                    .externalImageUrl("https://images.unsplash.com/photo-1565191999001-551c187427bb?auto=format&fit=crop&w=800")
                    .author("Admin Netra")
                    .published(true)
                    .build(),

                Article.builder()
                    .title("Inovasi Aspal Plastik: Jalan Raya yang Lebih Kuat")
                    .content("Sains - Penerapan aspal campuran plastik untuk pengerasan jalan mulai diuji coba di beberapa kota besar. Hasil penelitian menunjukkan jalan menjadi lebih tahan terhadap air dan beban berat.")
                    .type(ArticleType.NEWS)
                    .externalImageUrl("https://images.unsplash.com/photo-1545143333-6403a7bd74ea?auto=format&fit=crop&w=800")
                    .author("Admin Netra")
                    .published(true)
                    .build()
            );

            articleRepository.saveAllAndFlush(articles);
            log.info(">>>> [SEEDER] Saved {} articles. DB Count: {}", articles.size(), articleRepository.count());
            
        } catch (Exception e) {
            log.error(">>>> [SEEDER] ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
