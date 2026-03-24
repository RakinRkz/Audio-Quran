package com.audioquran.data.model

import android.net.Uri

/**
 * Represents a Surah (chapter) of the Quran with its audio file information.
 */
data class Surah(
    val number: Int,
    val name: String,
    val arabicName: String,
    val filePath: String,
    val fileUri: Uri,
    val duration: Long = 0L
) {
    companion object {
        // Standard Quran Surah names with their Arabic names
        val SURAH_NAMES = mapOf(
            1 to ("Al-Fatihah" to "الفاتحة"),
            2 to ("Al-Baqarah" to "البقرة"),
            3 to ("Al-E-Imran" to "آل عمران"),
            4 to ("An-Nisa" to "النساء"),
            5 to ("Al-Maidah" to "المائدة"),
            6 to ("Al-An'am" to "الأنعام"),
            7 to ("Al-A'raf" to "الأعراف"),
            8 to ("Al-Anfal" to "الأنفال"),
            9 to ("At-Tawbah" to "التوبة"),
            10 to ("Yunus" to "يونس"),
            11 to ("Hud" to "هود"),
            12 to ("Yusuf" to "يوسف"),
            13 to ("Ar-Ra'd" to "الرعد"),
            14 to ("Ibrahim" to "ابراهيم"),
            15 to ("Al-Hijr" to "الحجر"),
            16 to ("An-Nahl" to "النحل"),
            17 to ("Bani Israil" to "الإسراء"),
            18 to ("Al-Kahf" to "الكهف"),
            19 to ("Maryam" to "مريم"),
            20 to ("Ta-Ha" to "طه"),
            21 to ("Al-Anbiya" to "الأنبياء"),
            22 to ("Al-Hajj" to "الحج"),
            23 to ("Al-Mu'minun" to "المؤمنون"),
            24 to ("An-Nur" to "النور"),
            25 to ("Al-Furqan" to "الفرقان"),
            26 to ("Ash-Shu'ara" to "الشعراء"),
            27 to ("An-Naml" to "النمل"),
            28 to ("Al-Qasas" to "القصص"),
            29 to ("Al-Ankabut" to "العنكبوت"),
            30 to ("Ar-Rum" to "الروم"),
            31 to ("Luqman" to "لقمان"),
            32 to ("As-Sajdah" to "السجدة"),
            33 to ("Al-Ahzab" to "الأحزاب"),
            34 to ("Saba" to "سبأ"),
            35 to ("Fatir" to "فاطر"),
            36 to ("Ya-Sin" to "يس"),
            37 to ("As-Saffat" to "الصافات"),
            38 to ("Sad" to "ص"),
            39 to ("Az-Zumar" to "الزمر"),
            40 to ("Ghafir" to "غافر"),
            41 to ("Fussilat" to "فصلت"),
            42 to ("Ash-Shura" to "الشورى"),
            43 to ("Az-Zukhruf" to "الزخرف"),
            44 to ("Ad-Dukhan" to "الدخان"),
            45 to ("Al-Jathiyah" to "الجاثية"),
            46 to ("Al-Ahqaf" to "الأحقاف"),
            47 to ("Muhammad" to "محمد"),
            48 to ("Al-Fath" to "الفتح"),
            49 to ("Al-Hujurat" to "الحجرات"),
            50 to ("Qaf" to "ق"),
            51 to ("Adh-Dhariyat" to "الذاريات"),
            52 to ("At-Tur" to "الطور"),
            53 to ("An-Najm" to "النجم"),
            54 to ("Al-Qamar" to "القمر"),
            55 to ("Ar-Rahman" to "الرحمن"),
            56 to ("Al-Waqi'ah" to "الواقعة"),
            57 to ("Al-Hadid" to "الحديد"),
            58 to ("Al-Mujadila" to "المجادلة"),
            59 to ("Al-Hashr" to "الحشر"),
            60 to ("Al-Mumtahanah" to "الممتحنة"),
            61 to ("As-Saf" to " الصف"),
            62 to ("Al-Jumu'ah" to "الجمعة"),
            63 to ("Al-Munafiqun" to "المنافقون"),
            64 to ("At-Taghabun" to "التغابن"),
            65 to ("At-Talaq" to "الطلاق"),
            66 to ("At-Tahrim" to "التحريم"),
            67 to ("Al-Mulk" to "الملك"),
            68 to ("Al-Qalam" to "القلم"),
            69 to ("Al-Haqqah" to "الحاقة"),
            70 to ("Al-Ma'arij" to "المعارج"),
            71 to ("Nuh" to "نوح"),
            72 to ("Al-Jinn" to "الجن"),
            73 to ("Al-Muzzammil" to "المزمل"),
            74 to ("Al-Muddaththir" to "المدثر"),
            75 to ("Al-Qiyamah" to "القيامة"),
            76 to ("Al-Insan" to "الإنسان"),
            77 to ("Al-Mursalat" to "المرسلات"),
            78 to ("An-Naba" to "النبأ"),
            79 to ("An-Nazi'at" to "النازعات"),
            80 to ("Abasa" to "عبس"),
            81 to ("At-Takwir" to "التكوير"),
            82 to ("Al-Infitar" to "الإنفطار"),
            83 to ("Al-Mutaffifin" to "المطففين"),
            84 to ("Al-Inshiqaq" to "الإنشقاق"),
            85 to ("Al-Buruj" to "البروج"),
            86 to ("At-Tariq" to "الطارق"),
            87 to ("Al-Ala" to "الأعلى"),
            88 to ("Al-Ghashiyah" to "الغاشية"),
            89 to ("Al-Fajr" to "الفجر"),
            90 to ("Al-Balad" to "البلد"),
            91 to ("Ash-Shams" to "الشمس"),
            92 to ("Al-Layl" to "الليل"),
            93 to ("Ad-Duhaa" to "الضحى"),
            94 to ("Ash-Sharh" to "الشرح"),
            95 to ("At-Tin" to "التين"),
            96 to ("Al-Alaq" to "العلق"),
            97 to ("Al-Qadr" to "القدر"),
            98 to ("Al-Bayyinah" to "البينة"),
            99 to ("Az-Zalzalah" to "الزلزلة"),
            100 to ("Al-Adiyat" to "العاديات"),
            101 to ("Al-Qari'ah" to "القارعة"),
            102 to ("At-Takathur" to "التكاثر"),
            103 to ("Al-Asr" to "العصر"),
            104 to ("Al-Humazah" to "همزة"),
            105 to ("Al-Fil" to "الفيل"),
            106 to ("Quraysh" to "قريش"),
            107 to ("Al-Ma'un" to "الماعون"),
            108 to ("Al-Kawthar" to "الكوثر"),
            109 to ("Al-Kafirun" to "الكافرون"),
            110 to ("An-Nasr" to "النصر"),
            111 to ("Al-Lahab" to "المسد"),
            112 to ("Al-Ikhlas" to "الإخلاص"),
            113 to ("Al-Falaq" to "الفلق"),
            114 to ("An-Nas" to "الناس")
        )

        /**
         * Parse surah number from filename.
         * Expected format: "001 - Surah Name.m4a"
         */
        fun parseSurahNumber(filename: String): Int? {
            val pattern = Regex("^(\\d{1,3})\\s*[-–—]\\s*")
            val match = pattern.find(filename)
            return match?.groupValues?.get(1)?.toIntOrNull()
        }

        /**
         * Parse surah name from filename.
         * Expected format: "001 - Surah Name with bangla translation - recited by ..."
         */
        fun parseSurahName(filename: String): String {
            val numberPattern = Regex("^(\\d{1,3})\\s*[-–—]\\s*")
            val withoutNumber = numberPattern.replace(filename, "")
            
            // Remove the "with bangla translation" and "recited by..." parts
            val translationPattern = Regex("\\s*with\\s*bangla\\s*translation.*", RegexOption.IGNORE_CASE)
            val withoutTranslation = translationPattern.replace(withoutNumber, "")
            
            // Remove "recited by..." part
            val reciterPattern = Regex("\\s*[-–—]?\\s*recited\\s*by.*", RegexOption.IGNORE_CASE)
            val cleanName = reciterPattern.replace(withoutTranslation, "")
            
            // Remove file extension
            val extensionPattern = Regex("\\.(m4a|mp3|ogg|wav)$", RegexOption.IGNORE_CASE)
            return extensionPattern.replace(cleanName, "").trim()
        }
    }
}
