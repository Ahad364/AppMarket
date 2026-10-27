package com.appmarket.store.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** Current language code provided by MainActivity: "bn" or "en". */
val LocalAppLang = compositionLocalOf { "bn" }

/** Picks the Bangla or English text for the current language. */
@Composable
fun tr(bn: String, en: String): String = if (LocalAppLang.current == "en") en else bn
