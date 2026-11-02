package de.geosphere.congregationplaner.ui.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Abstand, den Inhalt oben freihalten muss: Kamera-Aussparung oder, wenn sie gerade eingeblendet ist, die
 * Statusleiste. Die App läuft im Vollbild (die Leisten sind ausgeblendet), die Aussparung bleibt aber.
 * Auf dem Desktop ist er 0.
 */
@Composable
fun topCutoutInset(): Dp = WindowInsets.displayCutout.union(WindowInsets.statusBars).asPaddingValues().calculateTopPadding()
