package com.fountofhopedotorg.fohbible.utils

object BibleVersionUtils {
    val versionMap: Map<String, String> = mapOf(
        "afr.sqlite3" to "AFR",
        "ampc.sqlite3" to "AMPC",
        "avd.sqlite3" to "AVD",
        "cebB.sqlite3" to "CEBB",
        "cjb.sqlite3" to "CJB",
        "csb17.sqlite3" to "CSB17",
        "cuv23.sqlite3" to "CUV23",
        "esv.sqlite3" to "ESV",
        "esvgsb.sqlite3" to "ESVGSB",
        "fpb.sqlite3" to "FPB",
        "hilab82.sqlite3" to "HILAB",
        "iesv+.sqlite3" to "IESV+",
        "ihot+.sqlite3" to "IHOT+",
        "kj2.sqlite3" to "KJ2",
        "kjv+.sqlite3" to "KJV+",
        "lccmn.sqlite3" to "LCCMN",
        "logos.sqlite3" to "Logos",
        "mbb05.sqlite3" to "MBB",
        "mhb.sqlite3" to "MHB",
        "mhbc.sqlite3" to "MHBC",
        "nasb+.sqlite3" to "NASB+",
        "niv11.sqlite3" to "NIV11",
        "nkjv.sqlite3" to "NKJV",
        "nlt15.sqlite3" to "NLT15",
        "nvb.sqlite3" to "NVB",
        "ojb.sqlite3" to "OJB",
        "РБО2.sqlite3" to "РБО2",
        "rst+.sqlite3" to "RST+",
        "sab.sqlite3" to "SAB",
        "sfilos.sqlite3" to "SFILOS",
        "tagab01.sqlite3" to "TAGAB",
        "tjb.sqlite3" to "TJB",
        "vmd.sqlite3" to "VMD",
        "wmb.sqlite3" to "WMB",
        "woy.sqlite3" to "WOY",
        "ylt.sqlite3" to "YLT",
        "УБД96.sqlite3" to "УБД96"
    )

    val descriptionMap: Map<String, String> = mapOf(
        "afr.sqlite3" to "Bible Afrikaans",
        "ampc.sqlite3" to "Amplified Bible Classic Edition",
        "avd.sqlite3" to "New Van Dyck Bible",
        "cebB.sqlite3" to "Cebuano Bible",
        "cjb.sqlite3" to "Complete Jewish Bible",
        "csb17.sqlite3" to "Christian Standard Bible",
        "cuv23.sqlite3" to "Ukrainian Contemporary Version",
        "esv.sqlite3" to "English Standard Version",
        "esvgsb.sqlite3" to "ESV Global Study Bible",
        "fpb.sqlite3" to "Filos Pergamos Bible",
        "hilab82.sqlite3" to "Hiligaynon Ang Biblia 1982",
        "iesv+.sqlite3" to "Greek-English Interlinear NT",
        "ihot+.sqlite3" to "Hebrew-English Interlinear OT",
        "kj2.sqlite3" to "King James 2000",
        "kjv+.sqlite3" to "King James Version 1769",
        "lccmn.sqlite3" to "Lời Chúa Cho Mọi Người",
        "logos.sqlite3" to "Logos Bible",
        "mbb05.sqlite3" to "Magandang Balita Biblia 2005",
        "mhb.sqlite3" to "Modern Hebrew Bible (Consonants)",
        "mhbc.sqlite3" to "Modern Hebrew Bible with Vowels",
        "nasb+.sqlite3" to "New American Standard Bible",
        "niv11.sqlite3" to "New International Version",
        "nkjv.sqlite3" to "New King James Version",
        "nlt15.sqlite3" to "New Living Translation",
        "nlv11.sqlite3" to "Nuwe Lewende Vertaling",
        "nvb.sqlite3" to "New Vietnamese Bible",
        "ojb.sqlite3" to "Orthodox Jewish Bible",
        "РБО2.sqlite3" to "The Modern Russian Translation",
        "rst+.sqlite3" to "Russian Synodal Bible",
        "sab.sqlite3" to "Sharif Arabic Bible",
        "sfilos.sqlite3" to "Σπύρος Φίλος",
        "tagab01.sqlite3" to "Tagalog Biblia 2001",
        "tjb.sqlite3" to "Terjemahan Baru",
        "vmd.sqlite3" to "Versi Mudah Dibaca",
        "wmb.sqlite3" to "World Messianic Bible",
        "woy.sqlite3" to "Word of Yahweh",
        "ylt.sqlite3" to "Young's Literal Translation",
        "УБД96.sqlite3" to "The Holy Bible in Ukrainian"
    )

    private val versionLanguage: Map<String, String> = mapOf(
        "afr.sqlite3" to "Afrikaans",
        "ampc.sqlite3" to "English",
        "avd.sqlite3" to "Arabic",
        "cebB.sqlite3" to "Cebuano",
        "cjb.sqlite3" to "English Messianic",
        "csb17.sqlite3" to "English",
        "cuv23.sqlite3" to "Ukrainian",
        "esv.sqlite3" to "English",
        "esvgsb.sqlite3" to "English",
        "fpb.sqlite3" to "Greek",
        "hilab82.sqlite3" to "Hiligaynon",
        "iesv+.sqlite3" to "Greek/Hebrew Interlinear",
        "ihot+.sqlite3" to "Greek/Hebrew Interlinear",
        "kj2.sqlite3" to "English",
        "kjv+.sqlite3" to "English",
        "lccmn.sqlite3" to "Vietnamese",
        "logos.sqlite3" to "English",
        "mbb05.sqlite3" to "Tagalog",
        "mhb.sqlite3" to "Hebrew",
        "mhbc.sqlite3" to "Hebrew",
        "nasb+.sqlite3" to "English",
        "niv11.sqlite3" to "English",
        "nkjv.sqlite3" to "English",
        "nlt15.sqlite3" to "English",
        "nlv11.sqlite3" to "Afrikaans",
        "nvb.sqlite3" to "Vietnamese",
        "ojb.sqlite3" to "English Messianic",
        "РБО2.sqlite3" to "Russian",
        "rst+.sqlite3" to "Russian",
        "sab.sqlite3" to "Arabic",
        "sfilos.sqlite3" to "Greek",
        "tagab01.sqlite3" to "Tagalog",
        "tjb.sqlite3" to "Indonesian",
        "vmd.sqlite3" to "Indonesian",
        "wmb.sqlite3" to "English Messianic",
        "woy.sqlite3" to "English Messianic",
        "ylt.sqlite3" to "English",
        "УБД96.sqlite3" to "Ukrainian"
    )

    fun getVersionsGroupedByLanguage(): Map<String, List<Pair<String, String>>> {
        val groups = mutableMapOf<String, MutableList<Pair<String, String>>>()
        versionMap.forEach { (key, shortName) ->
            val lang = versionLanguage[key] ?: "Other"
            groups.getOrPut(lang) { mutableListOf() }.add(key to shortName)
        }
        groups.forEach { (_, list) -> list.sortBy { it.second } }
        val order = listOf("English", "English Messianic") + groups.keys.filter { it != "English" && it != "English Messianic" }.sorted()
        return groups.toSortedMap(compareBy { order.indexOf(it).takeIf { idx -> idx >=0 } ?: Int.MAX_VALUE })
    }
}

////copyright-free versions
//object BibleVersionUtils {
//    val versionMap: Map<String, String> = mapOf(
//        "cebB.sqlite3" to "CEBB",
//        "cjb.sqlite3" to "CJB",
//        "hilab82.sqlite3" to "HILAB",
//        "iesv+.sqlite3" to "IESV+",
//        "ihot+.sqlite3" to "IHOT+",
//        "kj2.sqlite3" to "KJ2",
//        "kjv+.sqlite3" to "KJV+",
//        "logos.sqlite3" to "Logos",
//        "mbb05.sqlite3" to "MBB",
//        "nlt15.sqlite3" to "NLT15",
//        "ojb.sqlite3" to "OJB",
//        "tagab01.sqlite3" to "TAGAB",
//        "wmb.sqlite3" to "WMB",
//        "woy.sqlite3" to "WOY",
//        "ylt.sqlite3" to "YLT"
//    )
//
//    val descriptionMap: Map<String, String> = mapOf(
//        "cebB.sqlite3" to "Cebuano Bible",
//        "cjb.sqlite3" to "Complete Jewish Bible",
//        "hilab82.sqlite3" to "Hiligaynon Ang Biblia 1982",
//        "iesv+.sqlite3" to "Greek-English Interlinear NT",
//        "ihot+.sqlite3" to "Hebrew-English Interlinear OT",
//        "kj2.sqlite3" to "King James 2000",
//        "kjv+.sqlite3" to "King James Version 1769",
//        "logos.sqlite3" to "Logos Bible",
//        "mbb05.sqlite3" to "Magandang Balita Biblia 2005",
//        "nlt15.sqlite3" to "New Living Translation",
//        "ojb.sqlite3" to "Orthodox Jewish Bible",
//        "tagab01.sqlite3" to "Tagalog Biblia 2001",
//        "wmb.sqlite3" to "World Messianic Bible",
//        "woy.sqlite3" to "Word of Yahweh",
//        "ylt.sqlite3" to "Young's Literal Translation"
//    )
//
//    private val versionLanguage: Map<String, String> = mapOf(
//        "cebB.sqlite3" to "Cebuano",
//        "cjb.sqlite3" to "English",
//        "hilab82.sqlite3" to "Hiligaynon",
//        "iesv+.sqlite3" to "Greek/Hebrew Interlinear",
//        "ihot+.sqlite3" to "Greek/Hebrew Interlinear",
//        "kj2.sqlite3" to "English",
//        "kjv+.sqlite3" to "English",
//        "logos.sqlite3" to "English",
//        "mbb05.sqlite3" to "Tagalog",
//        "nlt15.sqlite3" to "English",
//        "ojb.sqlite3" to "English",
//        "tagab01.sqlite3" to "Tagalog",
//        "wmb.sqlite3" to "English",
//        "woy.sqlite3" to "English Messianic",
//        "ylt.sqlite3" to "English"
//    )
//
//    fun getVersionsGroupedByLanguage(): Map<String, List<Pair<String, String>>> {
//        val groups = mutableMapOf<String, MutableList<Pair<String, String>>>()
//        versionMap.forEach { (key, shortName) ->
//            val lang = versionLanguage[key] ?: "Other"
//            groups.getOrPut(lang) { mutableListOf() }.add(key to shortName)
//        }
//        groups.forEach { (_, list) -> list.sortBy { it.second } }
//        val order = listOf("English", "Greek/Hebrew Interlinear") + groups.keys.filter { it != "English" && it != "Greek/Hebrew Interlinear" }.sorted()
//        return groups.toSortedMap(compareBy { order.indexOf(it).takeIf { idx -> idx >=0 } ?: Int.MAX_VALUE })
//    }
//}