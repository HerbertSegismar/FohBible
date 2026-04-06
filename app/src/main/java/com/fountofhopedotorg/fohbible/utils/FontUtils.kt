package com.fountofhopedotorg.fohbible.utils

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

val availableFontFamilies = listOf(
    "system", "oswald", "rubikglitch", "rubiklines", "poppins",
    "cookie", "emilyscandy", "googlesanscode", "ptserif", "pirataone",
    "quintessential", "rougescript", "sairastencilone",
    "shadowsintolight", "smoochsans", "truculenta", "honk"
)

object Fonts {
    private val typefaceCache = mutableMapOf<String, Typeface>()

    fun getTypeface(context: Context, fontName: String): Typeface {
        return typefaceCache.getOrPut(fontName) {
            when (fontName) {
                "system" -> Typeface.DEFAULT
                "oswald" -> Typeface.createFromAsset(context.assets, "fonts/Oswald.ttf")
                "rubikglitch" -> Typeface.createFromAsset(context.assets, "fonts/RubikGlitch.ttf")
                "rubiklines" -> Typeface.createFromAsset(context.assets, "fonts/RubikLines.ttf")
                "poppins" -> Typeface.createFromAsset(context.assets, "fonts/Poppins.ttf")
                "cookie" -> Typeface.createFromAsset(context.assets, "fonts/Cookie.ttf")
                "emilyscandy" -> Typeface.createFromAsset(context.assets, "fonts/EmilysCandy.ttf")
                "googlesanscode" -> Typeface.createFromAsset(context.assets, "fonts/GoogleSansCode.ttf")
                "ptserif" -> Typeface.createFromAsset(context.assets, "fonts/PTSerif.ttf")
                "pirataone" -> Typeface.createFromAsset(context.assets, "fonts/PirataOne.ttf")
                "quintessential" -> Typeface.createFromAsset(context.assets, "fonts/Quintessential.ttf")
                "rougescript" -> Typeface.createFromAsset(context.assets, "fonts/RougeScript.ttf")
                "sairastencilone" -> Typeface.createFromAsset(context.assets, "fonts/SairaStencilOne.ttf")
                "shadowsintolight" -> Typeface.createFromAsset(context.assets, "fonts/ShadowsIntoLight.ttf")
                "smoochsans" -> Typeface.createFromAsset(context.assets, "fonts/SmoochSans.ttf")
                "truculenta" -> Typeface.createFromAsset(context.assets, "fonts/Truculenta.ttf")
                "honk" -> Typeface.createFromAsset(context.assets, "fonts/HonkVariable.ttf")
                else -> Typeface.DEFAULT
            }
        }
    }
}

@Composable
fun getFontFamily(fontName: String): FontFamily {
    val context = LocalContext.current
    return remember(fontName) {
        FontFamily(Fonts.getTypeface(context, fontName))
    }
}