package com.fountofhopedotorg.fohbible.utils

object InteractiveModalUtils {
    val dictionaries = listOf("atsbd", "cbtel", "isbe", "noah", "oxford", "topical", "tcr", "eunsa", "rbo", "gesenius", "ardbt")
    val dictionariesByLanguage: Map<String, List<String>> = mapOf(
        "en" to listOf("atsbd", "cbtel", "isbe", "noah", "oxford", "topical", "tcr"),
        "ar" to listOf("ardbt"),
        "es" to listOf("eunsa"),
        "heb" to listOf("gesenius"),
        "ru" to listOf("rbo"),
    )

    val dictionaryDisplayNames = mapOf(
        "atsbd" to "ATSBD",
        "cbtel" to "CBTEL",
        "eunsa" to "Sagrada Biblia Spanish Dictionary",
        "isbe" to "Int'l Standard Bible Encyclopedia",
        "noah" to "Noah Webster's Dictionary",
        "oxford" to "Oxford Dictionary",
        "rbo" to "Modern Russian Translation Dictionary",
        "topical" to "Topical Bible Dictionary",
        "tcr" to "Thompson Chain Reference",
        "gesenius" to "Hebrew and Chaldee Lexicon",
        "ardbt" to "Arabic Dictionary of Biblical Theology",
    )

    val verseCommentaries = listOf("cbsc", "spurgeon", "ebc", "fairbairn", "hawker", "mhwbc", "scofield", "tsk")
    val verseCommentaryDisplayNames = mapOf(
        "cbsc" to "Cambridge Bible Commentary",
        "spurgeon" to "Charles Spurgeon's Commentary",
        "ebc" to "Expositor's Bible Commentary",
        "fairbairn" to "Fairbairn's Typology of Scripture",
        "hawker" to "Hawker's Poor Man's Commentary",
        "mhwbc" to "Matthew Henry's Commentary",
        "scofield" to "Scofield Reference Bible",
        "tsk" to "Treasury of Scripture Knowledge"
    )

    val crossReferenceDatabases = listOf("esv", "niv11", "obx")
    val crossReferenceDatabaseDisplayNames = mapOf(
        "esv" to "References from ESV",
        "niv11" to "References from NIV",
        "obx" to "References from OpenBible Project"
    )
}