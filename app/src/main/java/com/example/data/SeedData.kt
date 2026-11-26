package com.example.data

import com.example.model.*
import com.example.util.Formatters

object SeedData {

    private fun variant(
        id: String,
        productId: String,
        namaVarian: String,
        sku: String,
        warnaNama: String,
        warnaHex: String,
        ukuran: String,
        stok: Int,
        harga1H: Long
    ): ProductVariant {
        val prices = Formatters.calculateAutoPrices(harga1H)
        return ProductVariant(
            id = id,
            productId = productId,
            namaVarian = namaVarian,
            sku = sku,
            warnaNama = warnaNama,
            warnaHex = warnaHex,
            ukuran = ukuran,
            stok = stok,
            harga1H = prices[0],
            harga2H = prices[1],
            harga3H = prices[2],
            harga4H = prices[3],
            harga5H = prices[4],
            harga6H = prices[5],
            status = VariantStatus.TERSEDIA
        )
    }

    fun initialProducts(): List<Product> = listOf(
        Product(
            id = "prod_tenda_4p",
            nama = "Tenda Dome 4P",
            kategori = "Tenda",
            deskripsi = "Tenda double layer kapasitas 4 orang, frame alloy tahan angin & hujan.",
            variants = listOf(
                variant("var_tnd4p_olv", "prod_tenda_4p", "Olive Trail", "TND4P-OLV", "Olive", "#556B2F", "4 Orang", 5, 65000),
                variant("var_tnd4p_mrn", "prod_tenda_4p", "Maroon Peak", "TND4P-MRN", "Maroon", "#571331", "4 Orang", 4, 65000),
                variant("var_tnd4p_mst", "prod_tenda_4p", "Kuning Mustard", "TND4P-MST", "Kuning", "#D97706", "4 Orang", 3, 65000)
            )
        ),
        Product(
            id = "prod_tenda_2p",
            nama = "Tenda Dome 2P",
            kategori = "Tenda",
            deskripsi = "Tenda ultralight double layer untuk 2 pendaki.",
            variants = listOf(
                variant("var_tnd2p_sge", "prod_tenda_2p", "Hijau Sage", "TND2P-SGE", "Hijau", "#4D7C0F", "2 Orang", 6, 45000),
                variant("var_tnd2p_org", "prod_tenda_2p", "Oranye Sunset", "TND2P-ORG", "Oranye", "#EA580C", "2 Orang", 4, 45000)
            )
        ),
        Product(
            id = "prod_carrier_60",
            nama = "Carrier 60L",
            kategori = "Tas & Carrier",
            deskripsi = "Tas gunung 60 liter dengan backsystem nyaman & raincover.",
            variants = listOf(
                variant("var_cr60_blk", "prod_carrier_60", "Hitam Onyx", "CR60-BLK", "Hitam", "#1F2937", "60L", 6, 40000),
                variant("var_cr60_nvy", "prod_carrier_60", "Biru Navy", "CR60-NVY", "Biru", "#1E3A8A", "60L", 4, 40000),
                variant("var_cr60_red", "prod_carrier_60", "Merah Bata", "CR60-RED", "Merah", "#991B1B", "60L", 3, 40000)
            )
        ),
        Product(
            id = "prod_carrier_45",
            nama = "Carrier 45L",
            kategori = "Tas & Carrier",
            deskripsi = "Carrier ringkas 45 liter cocok untuk pendakian 1-2 malam.",
            variants = listOf(
                variant("var_cr45_slt", "prod_carrier_45", "Abu Slate", "CR45-SLT", "Abu-Abu", "#475569", "45L", 5, 35000),
                variant("var_cr45_arm", "prod_carrier_45", "Hijau Army", "CR45-ARM", "Hijau", "#3F6212", "45L", 4, 35000)
            )
        ),
        Product(
            id = "prod_sb",
            nama = "Sleeping Bag",
            kategori = "Tidur",
            deskripsi = "Sleeping bag polar tebal tahan suhu dingin gunung.",
            variants = listOf(
                variant("var_sb_tel", "prod_sb", "Biru Teal", "SB-TEL", "Biru", "#0F766E", "Mummy Reguler", 10, 15000),
                variant("var_sb_mrn", "prod_sb", "Maroon Hangat", "SB-MRN", "Maroon", "#571331", "Mummy Reguler", 8, 15000)
            )
        ),
        Product(
            id = "prod_matras",
            nama = "Matras",
            kategori = "Tidur",
            deskripsi = "Matras spon anti lembap & ringan untuk alas tidur.",
            variants = listOf(
                variant("var_mtr_blk", "prod_matras", "Hitam Standar", "MTR-BLK", "Hitam", "#111827", "180x60cm", 15, 7000),
                variant("var_mtr_blu", "prod_matras", "Biru Foil", "MTR-BLU", "Biru", "#2563EB", "180x60cm", 10, 10000)
            )
        ),
        Product(
            id = "prod_kompor",
            nama = "Kompor Portable",
            kategori = "Masak",
            deskripsi = "Kompor windproof lipat dengan pemantik piezo.",
            variants = listOf(
                variant("var_kmp_red", "prod_kompor", "Merah Windproof", "KMP-RED", "Merah", "#DC2626", "Compact", 8, 15000),
                variant("var_kmp_blk", "prod_kompor", "Hitam Kotak", "KMP-BLK", "Hitam", "#1F2937", "Compact", 6, 15000)
            )
        ),
        Product(
            id = "prod_cooking",
            nama = "Cooking Set",
            kategori = "Masak",
            deskripsi = "Nesting panci + teflon + mangkuk untuk 3-4 orang.",
            variants = listOf(
                variant("var_ckt_slv", "prod_cooking", "Abu Silver DS-300", "CKT-SLV", "Abu-Abu", "#64748B", "Set 3-4P", 7, 15000),
                variant("var_ckt_org", "prod_cooking", "Oranye Anodized", "CKT-ORG", "Oranye", "#EA580C", "Set 3-4P", 5, 18000)
            )
        ),
        Product(
            id = "prod_lampu",
            nama = "Lampu Tenda",
            kategori = "Penerangan",
            deskripsi = "Lampu gantung LED rechargeable tahan hingga 12 jam.",
            variants = listOf(
                variant("var_lmp_ylw", "prod_lampu", "Kuning Hangat", "LMP-YLW", "Kuning", "#CA8A04", "USB-C", 12, 10000),
                variant("var_lmp_blk", "prod_lampu", "Hitam Tactical", "LMP-BLK", "Hitam", "#1F2937", "USB-C", 8, 10000)
            )
        ),
        Product(
            id = "prod_kursi",
            nama = "Kursi Lipat",
            kategori = "Furnitur",
            deskripsi = "Kursi camping lipat rangka aluminium kuat hingga 110 kg.",
            variants = listOf(
                variant("var_krs_krm", "prod_kursi", "Krem Khaki", "KRS-KRM", "Krem", "#A16207", "Standar", 8, 20000),
                variant("var_krs_blk", "prod_kursi", "Hitam Matte", "KRS-BLK", "Hitam", "#1F2937", "Standar", 10, 20000),
                variant("var_krs_olv", "prod_kursi", "Olive Green", "KRS-OLV", "Olive", "#556B2F", "Standar", 6, 20000)
            )
        ),
        Product(
            id = "prod_meja",
            nama = "Meja Lipat",
            kategori = "Furnitur",
            deskripsi = "Meja camping lipat portabel ringan dengan tas penyimpanan.",
            variants = listOf(
                variant("var_mja_blk", "prod_meja", "Hitam Alloy", "MJA-BLK", "Hitam", "#1F2937", "Medium", 6, 25000),
                variant("var_mja_brw", "prod_meja", "Coklat Kayu", "MJA-BRW", "Coklat", "#78350F", "Large", 4, 30000)
            )
        ),
        Product(
            id = "prod_tp",
            nama = "Trekking Pole",
            kategori = "Trekking",
            deskripsi = "Tongkat gunung aluminium telescopic dengan sistem kunci cepat.",
            variants = listOf(
                variant("var_tp_red", "prod_tp", "Merah Crimson", "TP-RED", "Merah", "#B91C1C", "65-135cm", 10, 12000),
                variant("var_tp_blu", "prod_tp", "Biru Glacier", "TP-BLU", "Biru", "#1D4ED8", "65-135cm", 8, 12000),
                variant("var_tp_blk", "prod_tp", "Hitam Carbon", "TP-BLK", "Hitam", "#1F2937", "65-135cm", 6, 15000)
            )
        )
    )

    fun initialPackages(): List<RentalPackage> {
        val p1 = Formatters.calculateAutoPrices(80000)
        val p2 = Formatters.calculateAutoPrices(135000)
        return listOf(
            RentalPackage(
                id = "pkg_camp_2p",
                nama = "Paket Camp Berdua 2P",
                deskripsi = "Tenda 2P + 2 Sleeping Bag + 2 Matras + Lampu Tenda",
                harga1H = p1[0],
                harga2H = p1[1],
                harga3H = p1[2],
                harga4H = p1[3],
                harga5H = p1[4],
                harga6H = p1[5],
                items = listOf(
                    PackageItem("prod_tenda_2p", "Tenda Dome 2P", "var_tnd2p_sge", "Hijau Sage", "TND2P-SGE", 1),
                    PackageItem("prod_sb", "Sleeping Bag", "var_sb_tel", "Biru Teal", "SB-TEL", 2),
                    PackageItem("prod_matras", "Matras", "var_mtr_blk", "Hitam Standar", "MTR-BLK", 2),
                    PackageItem("prod_lampu", "Lampu Tenda", "var_lmp_ylw", "Kuning Hangat", "LMP-YLW", 1)
                ),
                aktif = true
            ),
            RentalPackage(
                id = "pkg_summit_4p",
                nama = "Paket Summit 4P Lengkap",
                deskripsi = "Tenda 4P + 4 Sleeping Bag + Kompor + Cooking Set + Lampu",
                harga1H = p2[0],
                harga2H = p2[1],
                harga3H = p2[2],
                harga4H = p2[3],
                harga5H = p2[4],
                harga6H = p2[5],
                items = listOf(
                    PackageItem("prod_tenda_4p", "Tenda Dome 4P", "var_tnd4p_olv", "Olive Trail", "TND4P-OLV", 1),
                    PackageItem("prod_sb", "Sleeping Bag", "var_sb_mrn", "Maroon Hangat", "SB-MRN", 4),
                    PackageItem("prod_kompor", "Kompor Portable", "var_kmp_red", "Merah Windproof", "KMP-RED", 1),
                    PackageItem("prod_cooking", "Cooking Set", "var_ckt_slv", "Abu Silver DS-300", "CKT-SLV", 1),
                    PackageItem("prod_lampu", "Lampu Tenda", "var_lmp_ylw", "Kuning Hangat", "LMP-YLW", 1)
                ),
                aktif = true
            )
        )
    }

    fun initialEmployees(): List<Employee> = listOf(
        Employee("emp_admin_1", "Budi (Admin)", UserRole.ADMIN, "1234", "081234567890", true),
        Employee("emp_kasir_1", "Rina (Kasir)", UserRole.KASIR, "0000", "081398765432", true),
        Employee("emp_kasir_2", "Dimas (Kasir)", UserRole.KASIR, "0000", "085712345678", true)
    )

    fun initialSettings(): AppSettingsBundle = AppSettingsBundle()
}
