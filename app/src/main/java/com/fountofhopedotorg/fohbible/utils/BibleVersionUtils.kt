package com.fountofhopedotorg.fohbible.utils

object BibleVersionUtils {
    val versionMap: Map<String, String> = mapOf(
        "ampc.sqlite3" to "AMPC",
        "cebB.sqlite3" to "CEBB",
        "csb17.sqlite3" to "CSB17",
        "esv.sqlite3" to "ESV",
        "esvgsb.sqlite3" to "ESVGSB",
        "hilab82.sqlite3" to "HILAB",
        "kj2.sqlite3" to "KJ2",
        "kjv+.sqlite3" to "KJV+",
        "logos.sqlite3" to "Logos",
        "nasb+.sqlite3" to "NASB+",
        "niv11.sqlite3" to "NIV11",
        "nkjv.sqlite3" to "NKJV",
        "nlt15.sqlite3" to "NLT15",
        "tagab01.sqlite3" to "TAGAB",
        "ylt.sqlite3" to "YLT"
    )
    val descriptionMap: Map<String, String> = mapOf(
        "ampc.sqlite3" to "Amplified Bible Classic Edition",
        "cebB.sqlite3" to "Cebuano Bible",
        "csb17.sqlite3" to "Christian Standard Bible",
        "esv.sqlite3" to "English Standard Version",
        "esvgsb.sqlite3" to "ESV Global Study Bible",
        "hilab82.sqlite3" to "Hiligaynon Ang Biblia 1982",
        "kj2.sqlite3" to "King James 2000",
        "kjv+.sqlite3" to "King James Version 1769",
        "logos.sqlite3" to "Logos Bible",
        "nasb+.sqlite3" to "New American Standard Bible",
        "niv11.sqlite3" to "New International Version",
        "nkjv.sqlite3" to "New King James Version",
        "nlt15.sqlite3" to "New Living Translation",
        "tagab01.sqlite3" to "Tagalog Biblia 2001",
        "ylt.sqlite3" to "Young's Literal Translation"
    )
}