package de.geosphere.congregationplaner.notice.domain.model

sealed class TalkNoticeException(message: String) : Exception(message) {
    class NotAllowed : TalkNoticeException("Dafür fehlt dir die Berechtigung.")

    class InvalidTitle : TalkNoticeException("Die Überschrift muss 1 bis $MAX_TITLE_LENGTH Zeichen haben.")

    class InvalidImageName :
        TalkNoticeException("Der Name der Grafik muss 1 bis $MAX_IMAGE_NAME_LENGTH Zeichen haben.")

    class ImageTooLarge : TalkNoticeException("Die Grafik ist größer als 700 KB.")

    class ImageTooLargePixels :
        TalkNoticeException("Die Grafik ist größer als $MAX_IMAGE_PIXELS × $MAX_IMAGE_PIXELS Pixel.")

    class NotAPng : TalkNoticeException("Die Datei ist keine PNG-Grafik.")

    class TooManyImages : TalkNoticeException("Es sind höchstens $MAX_IMAGES Grafiken möglich.")
}
