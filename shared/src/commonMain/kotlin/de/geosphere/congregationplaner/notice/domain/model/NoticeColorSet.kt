package de.geosphere.congregationplaner.notice.domain.model

/** Color sets of the notice as ARGB values; the text color is the same for all sets. */
@Suppress("MagicNumber") // the ARGB hex literals are the palette itself
enum class NoticeColorSet(
    val key: String,
    val label: String,
    val primary: Long,
    val onPrimary: Long,
    val stripe: Long,
) {
    BLUE("blue", "Blau", 0xFF1F3864, 0xFFFFFFFF, 0xFFDFE6F5),
    GREEN("green", "Grün", 0xFF1E5631, 0xFFFFFFFF, 0xFFDCEFE2),
    BORDEAUX("bordeaux", "Bordeaux", 0xFF6D1A2C, 0xFFFFFFFF, 0xFFF3DFE3),
    ANTHRACITE("anthracite", "Anthrazit", 0xFF333A40, 0xFFFFFFFF, 0xFFE4E7EA),
    PETROL("petrol", "Petrol", 0xFF0F5257, 0xFFFFFFFF, 0xFFDCEEEF),
    ;

    companion object {
        const val TEXT = 0xFF404040

        fun fromKey(key: String?): NoticeColorSet = entries.firstOrNull { it.key == key } ?: BLUE
    }
}
