package com.poi.feature.localcalendar

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

enum class LocalObservanceKind {
    LOCAL,
    FESTIVAL,
    CIVIC,
}

data class LocalObservance(
    val title: String,
    val kind: LocalObservanceKind = LocalObservanceKind.LOCAL,
    val featured: Boolean = false,
)

data class LocalCalendarDay(
    val date: LocalDate,
    val sunrise: LocalTime,
    val sunset: LocalTime,
    val observances: List<LocalObservance>,
)

/**
 * January 2026 transcription supplied from the printed Sharada Calendar.
 *
 * Printed lines are kept as individual records rather than being split at commas. This preserves
 * the source wording and gives future review/import tooling a stable unit to compare.
 */
object January2026Calendar {
    const val SOURCE = "Sharada Calendar 2026"
    val month: YearMonth = YearMonth.of(2026, 1)

    private fun local(title: String, featured: Boolean = false) =
        LocalObservance(title, LocalObservanceKind.LOCAL, featured)

    private fun festival(title: String, featured: Boolean = false) =
        LocalObservance(title, LocalObservanceKind.FESTIVAL, featured)

    private fun civic(title: String, featured: Boolean = false) =
        LocalObservance(title, LocalObservanceKind.CIVIC, featured)

    private val observancesByDay = mapOf(
        1 to listOf(
            festival("ಪ್ರದೋಷ, ಪದ್ಯಾಣ ಮಹಾಲಿಂಗೇಶ್ವರ ಉತ್ಸವ"),
            civic("ಕ್ರಿಸ್ತಶಕ 2026 ಆರಂಭ, ತಡಗಜಿ ದೈವ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ"),
            local("ಮಾಯಿಪ್ಪಾಡಿ ವಿಷ್ಣುಮೂರ್ತಿ ಜಾತ್ರಾರಂಭ"),
        ),
        2 to listOf(
            local("ಮಾಯಿಪ್ಪಾಡಿ ವಿಷ್ಣುಮೂರ್ತಿ ಒತ್ತೆಕೋಲ"),
            festival("ಕೊಲ್ಲೂರು ಮಂಡಲಪೂಜೆ, ಶಾಕಂಬರಿ ಚತುರ್ಥಿ"),
            local("ಮಣ್ಗಡ್ಡೆ ನವದುರ್ಗಾಗಣಪತಿ ದೇವಳದಲ್ಲಿ ಚಂಡಿಕಾಯಾಗ"),
        ),
        3 to listOf(
            festival("ಪೌರ್ಣಮಿ, ಪದ್ಯಾಣ ಧೂಮಾವತಿ ನೇಮೋತ್ಸವ"),
            local("ಎರ್ಮಾರ್ ಮಂಡಲಪೂಜೆ, ಕುನಾರು ಅಮ್ಮಂಬಳ ಸೋಮನಾಥೇಶ್ವರ ಮಂಡಲಪೂಜೆ"),
            local("ಬಜ್ಪೆ ಶನೈಶ್ಚರ ದೇವರ ವರ್ಧಂತ್ಯುತ್ಸವ"),
        ),
        4 to listOf(
            local("ಉಳ್ಳಾಲ ಉಳ್ಳಾಲ್ತಿ ಧರ್ಮ ಅರಸರ ಶ್ರೀ ದುರ್ಗಾಹೋಮ"),
        ),
        5 to listOf(
            local("ಕೇರಿಂಜ ಉಳ್ಳಾಲ್ತಿ ನವೀಕರಣ ವಾರ್ಷಿಕೋತ್ಸವ"),
            local("ಕೊಡವೂರು ಶಂಕರನಾರಾಯಣ ರಥ"),
            local("ಕಂಕನಾಡಿ ಬ್ರಹ್ಮಬೈದರ್ಕಳ ಜಾತ್ರೆ"),
            local("ಉಳ್ಳಾಲ ಉಳ್ಳಾಲ್ತಿ ಧರ್ಮ ಅರಸರ ಗಣಪತಿ ಹೋಮ"),
        ),
        6 to listOf(
            festival("ಸಂಕಷ್ಟ ಚತುರ್ಥಿ – ಚಂ.ಉ.ಫಂ. 9–20"),
        ),
        8 to listOf(
            local("ಮದಗ (ಕುಂಭಾರು) ಜನಾರ್ದನ ರಥ"),
        ),
        9 to listOf(
            local("ಉಡುಪಿ ಸಪ್ತೋತ್ಸವಾರಂಭ, ಕೋಟೆ ಅಮ್ಮತೇರು ಜಾತ್ರೆ"),
            local("ಉಳ್ಳಾಲ ಉಳ್ಳಾಲ್ತಿ ಧರ್ಮ ಅರಸರ ಚಂಡಿಕಾಯಾಗ"),
        ),
        10 to listOf(
            local("ಸುಳ್ಯ ಚೆನ್ನಕೇಶವ ರಥ"),
        ),
        11 to listOf(
            local("ಶ್ರೀ ವೀರವೆಂಕಟೇಶ ಪುನಃಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ, ಮಹಾನಕ್ಷತ್ರ ಉತ್ತರಾಭಾದ್ರ ಪ್ರಾರಂಭ"),
        ),
        12 to listOf(
            civic("ಸ್ವಾಮಿ ವಿವೇಕಾನಂದ ಜನ್ಮದಿನ"),
        ),
        14 to listOf(
            festival("ಮಕರಸಂಕ್ರಮಣ", featured = true),
            festival("ಸರ್ವೈಕಾದಶಿ, ಮಕರಸಂಕ್ರಮಣ, ಕರಿ ತೀರ್ಥ, ಪೊಂಗಲ್, ಕುಂಬಳೆ ಧ್ವಜ"),
            local("ಸಜ್ಜಪಜಾತ್ರೆ, ಉಡುಪಿ ಮೂರು ರಥ, ಕಾವೂರು ಮಹಾಲಿಂಗೇಶ್ವರ ಧ್ವಜ"),
            local("ಕೊಡ್ಲು ವಿಷ್ಣುಮಂಗಲ ಮಹಾವಿಷ್ಣು ಜಾತ್ರೆ"),
            local("ಸಂಗಡಿಬಿಟ್ಟು ಬನಶಂಕರಿ ಉತ್ಸವ, ಬಜವಳ್ಳಿ ಅಯ್ಯಪ್ಪ ಮಹಾಭಿಷೇಕ"),
            local("ಶ್ರೀ ಕ್ಷೇತ್ರ ಒಂಬತ್ತು ಕೆರೆ ಚಂಡಿಕಾಯಾಗ"),
            local("ಕಾವೂರು ಮುಳ್ಳಕಾಡು ಅಯ್ಯಪ್ಪ ದೀಪೋತ್ಸವ"),
            local("ಕೋಡಿಕಲ್ ಜಿ.ಎಸ್.ಬಿ. ಸಾಮೂಹಿಕ ಸತ್ಯನಾರಾಯಣ ಪೂಜೆ"),
            local("ನಂದಾವರ ಶಂಕರನಾರಾಯಣ ಸತ್ಯನಾರಾಯಣ ಪೂಜೆ"),
            local("ಇರುವೈಲು ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಭಜನಾ ಮಂಗಲ"),
            local("ಕುಡ್ರೋಳಿ ಭಗವತಿ ಚಂಡಿಕಾಯಾಗ"),
            local("ಮಂಗಳೂರು ಮುನೀಶ್ವರ ಮಹಾಗಣಪತಿ ವಾರ್ಷಿಕೋತ್ಸವ"),
        ),
        15 to listOf(
            local("ಉಡುಪಿ ಚೂರ್ಣೋತ್ಸವ, ಕುಂಬಳೆ ಪುತಿಗೆ ಸುಬ್ರಾಯ ಮಹೋತ್ಸವ"),
            local("ಕುಂದಾಪುರ ಅರಸಮ್ಮಕಾನ ಶ್ರೀದೇವಿ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಕೊಂಡೋತ್ಸವ"),
            local("ಮಂಜನಾಡಿ ವಿಷ್ಣುಮೂರ್ತಿ ಜನಾರ್ದನ ಧ್ವಜ"),
            local("ಮಹಾಗಣಪತಿ ದೇವಸ್ಥಾನ ಉರ್ವಸ್ಟೋರ್ ನವಗ್ರಹ ಯಾಗ"),
        ),
        16 to listOf(
            local("ದೇಂತಡ್ಕ ವನದುರ್ಗಾ ಧ್ವಜ, ಕೆಮ್ಮಾಯಿ ಓಂ ಅಯ್ಯಪ್ಪ ವರ್ಧಂತಿ"),
            local("ಶಂಕರನಾರಾಯಣ–ಕುಂಭಾಶಿ–ಸಾಲಿಗ್ರಾಮ–ಧಾರೇಶ್ವರ ರಥ"),
        ),
        17 to listOf(
            festival("ಮಾಸಶಿವರಾತ್ರಿ, ಕಣ್ಣೂರು ಮಹೋತ್ಸವ, ಕುಂಬಳೆ ಬೆಡಿ ಉತ್ಸವ"),
            local("ದೇಂತಡ್ಕ ವನದುರ್ಗಾ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ, ಕಾವೂರು ಮಹಾಲಿಂಗೇಶ್ವರ ರಥ"),
            local("ನಾವೂರು ಕೊಡಿಬೈಲು ಮಹಮ್ಮಾಯಿ ಜಾತ್ರೆ"),
            local("ಕಟೀಲು ಮಾಲಿದೇವಿ ವಾರ್ಷಿಕ ಉತ್ಸವ ಆರಂಭ"),
        ),
        18 to listOf(
            festival("ಅಮಾವಾಸ್ಯೆ, ಪುರಂದರದಾಸರ ಪುಣ್ಯದಿನ, ದೇಂತಡ್ಕ ವನದುರ್ಗಾ ರಥ"),
            festival("ಮಾಘಸ್ನಾನಾರಂಭ, ಮಂಜನಾಡಿ ವಿಷ್ಣುಮೂರ್ತಿ ಜನಾರ್ದನ ಉತ್ಸವ"),
            festival("ಉಡುಪಿ ಶಿರೂರು ಪರ್ಯಾಯೋತ್ಸವ, ಪಂಚಗ್ರಹ ಯೋಗ"),
        ),
        19 to listOf(
            local("ದೇಂತಡ್ಕ ವನದುರ್ಗಾ ಅವಭೃತ, ಕಾನಂಗಿ ಶ್ರೀನಿವಾಸ ಧ್ವಜ"),
            local("ಕಮಾರಡು ಮಹಾವಿಷ್ಣು ಮಹೋತ್ಸವ"),
            local("ಸುರತ್ಕಲ್ಲು ವೀರಭದ್ರ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಧ್ವಜ"),
            local("ಮೀಂಜ ಕೊರಿಕ್ಕಾರು ವಿಷ್ಣುಮೂರ್ತಿ ಮಯ್ಯನಾಡು ದೈವ ಉತ್ಸವ"),
            local("ಮಂಗಳೂರು ಶ್ರೀ ವೀರವೆಂಕಟೇಶ ದೇವರಿಗೆ ಸಹಸ್ರ ಕುಂಭಾಭಿಷೇಕ"),
        ),
        20 to listOf(
            festival("ಚಂದ್ರದರ್ಶನ, ಮಂಗಳೂರು ಲಕ್ಷ್ಮೀ ನಾರಾಯಣ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ"),
        ),
        21 to listOf(
            local("ಉಜಿರೆ ಜನಾರ್ದನ ರಥ, ವಿಟ್ಲ ಪಂಚಲಿಂಗೇಶ್ವರ ರಥ"),
            local("ಕದ್ರಿ ರಥ, ಬಲಾಡು ವನಶಾಸ್ತಾರ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ"),
            local("ಉಪ್ಪಳ ಸಂತಡ ಅರಸು ಸಂಕಲ ದೈವ ಉತ್ಸವ"),
            local("ಕಚ್ಚೂರು ಮಾಲತಿದೇವಿ ಬಬ್ಬುಸ್ವಾಮಿ ಕೊಂಡಸೇವೆ"),
        ),
        22 to listOf(
            festival("ವಿನಾಯಕ ಚತುರ್ಥಿ, ಸೌತಡ್ಕ ಮಹಾಗಣಪತಿ ಮೂಡಪ್ಪ ಸೇವೆ"),
            local("ಕಟೀಲು ಮಹೋತ್ಸವ, ಮಹಾಗಣಪತಿ ದೇವಸ್ಥಾನ ಉರ್ವಸ್ಟೋರ್ ಮಂಗಳೂರು ವಾರ್ಷಿಕ ಜಾತ್ರೆ ಆರಂಭ"),
        ),
        23 to listOf(
            festival("ವಸಂತ ಪಂಚಮಿ, ಸುರತ್ಕಲ್ಲು ಪುರಾತನ ಮಾರಿಯಮ್ಮ ಚೂರ್ಣೋತ್ಸವ"),
            local("ಸುರತ್ಕಲ್ಲು ವೀರಭದ್ರ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಚೂರ್ಣೋತ್ಸವ"),
            local("ಬಸ್ರೂರು ಮಹಾಲಿಂಗ ನಾರಾಯಣಿ ರಥ, ಅಗಲಾಡಿ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಧ್ವಜ"),
            local("ಕಾಂಞಂಗಾಡ್ ಲಕ್ಷ್ಮೀ ವೆಂಕಟೇಶ ಭಜನಾ ಸಪ್ತಾಹಾರಂಭ"),
            local("ಶಿರಾಡಿ ಜಾತ್ರೆ, ಕುಂಗ್ಯಾ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಉತ್ಸವ"),
        ),
        24 to listOf(
            festival("ಮಹಾನಕ್ಷತ್ರ ಶ್ರವಣ ಪ್ರಾರಂಭ"),
            civic("ಕವಿ ಮುದ್ದಣ್ಣ ಜಯಂತಿ, ಕೊಡದಡ ಅನ್ನಪೂರ್ಣೆ ಉತ್ಸವಾರಂಭ"),
            local("ಕೊಡದಡ ಮಲರಾಯ ನೇಮೋತ್ಸವ, ಕಾರ್ಕಳ ದೇವಕೃಷ್ಣ ವರ್ಧಂತಿ"),
        ),
        25 to listOf(
            festival("Kodial Teru", featured = true),
            festival("ರಥಸಪ್ತಮಿ, ಮಂಗಳೂರು–ಕುಮಟಾ–ವೆಂಕಾಪುರ ರಥ"),
            local("ಮರೋಳಿ ಸೂರ್ಯನಾರಾಯಣ ರಥ"),
            local("ಕಾನಂಗಿ ಶ್ರೀನಿವಾಸ ರಥ, ದುರ್ಗಾ ಹರಿಹರೇಶ್ವರ ರಥ"),
            local("ಪೋಳ್ಯ ಲಕ್ಷ್ಮೀ ವೆಂಕಟರಮಣ ರಥ, ಪಣಂಬೂರು ವಿಷ್ಣುಮೂರ್ತಿ ರಥ"),
            local("ಕೊಲ್ಲಮೊಗರು ಕೊಚ್ಚಿಲ ಸುಬ್ರಹ್ಮಣ್ಯ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ"),
            local("ಕೊಡದಡ ಅನ್ನಪೂರ್ಣೆ ಕೆರೆ ಉತ್ಸವ"),
            local("ಉರ್ವಸ್ಟೋರ್ ಮಹಾಗಣಪತಿ ಹಗಲು ರಥೋತ್ಸವ"),
        ),
        26 to listOf(
            festival("ಭೀಷ್ಮಾಷ್ಟಮಿ, ಕೊಡದಡ ಅನ್ನಪೂರ್ಣೆ ರಥ, ಇಡಗುಂಜಿ ವಿನಾಯಕ ರಥ"),
            local("ಮಂಗಳೂರು ಪೊಲೀಸ್‌ಲೈನ್ ಮುನೀಶ್ವರ ಗಣಪತಿ ವಾರ್ಷಿಕೋತ್ಸವ"),
            civic("ಗಣರಾಜ್ಯ ದಿನ", featured = true),
            local("ಮಲಾರಬೀಡು ಸಾಮೂಹಿಕ ಕಾಲಾವಧಿ ಸತ್ಯನಾರಾಯಣ ಪೂಜೆ, ಗಣರಾಜ್ಯ ದಿನ"),
        ),
        27 to listOf(
            festival("ಮದ್ದನವಮಿ, ನಾಳ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ರಥ"),
            local("ಅಗಲಾಡಿ ಮಹೋತ್ಸವ, ಮುಗೇರಡ್ಕ ಶಿರಾಡಿ ದೈವ ಜಾತ್ರೆ"),
            local("ಮೊಗಸನಾಡು ರಥ, ಕಟಪಾಡಿ ರಥ"),
            local("ಕುಂಗ್ಯಾ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಜಾತ್ರೆ"),
            local("ಉಪ್ಪಿನಂಗಡಿ ಲಕ್ಷ್ಮೀ ವೆಂಕಟರಮಣ ರಥ"),
        ),
        28 to listOf(
            local("ಅಗಲಾಡಿ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಅವಭೃತ"),
            local("ಒಡಿಯೂರು ವಾರ್ಷಿಕೋತ್ಸವ ರಥ"),
            local("ಕಾನಂಗಿ ಗಾಯತ್ರಿದೇವಿ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ"),
            local("ಸುಕ್ಷೇತ್ರ ತಿಂಥಿಣಿ ಮೌನೇಶ್ವರ ಉತ್ಸವಾರಂಭ"),
        ),
        29 to listOf(
            festival("ಸರ್ವೈಕಾದಶಿ, ನಾರಂಪಾಡಿ ಉಮ್ಮಮಹೇಶ್ವರ ಧ್ವಜ"),
        ),
        30 to listOf(
            festival("ಪ್ರದೋಷ, ಶ್ರೀ ವಾದಿರಾಜ ಜಯಂತಿ"),
            local("ಬಲಾಡು ಬಟ್ಟೆ ವಿನಾಯಕ ಉತ್ಸವ"),
            local("ಬೇಕೂರು ಮುಕ್ಕಿಂಜ ಕಿನ್ನಿಮಾಣಿ ಉತ್ಸವ"),
            local("ಬೊಳೂರು ಜಾರಂದಾಯ ಬಂಡಿ ಉತ್ಸವ ಧ್ವಜಾರೋಹಣ"),
            local("ಕಳಸ ರಥ, ಕಾಂಞಂಗಾಡ್ ಭಜನಾ ಸಪ್ತಾಹ ಮಂಗಲ"),
        ),
        31 to listOf(
            local("ಕಲ್ಲೇಗ ಕಲ್ಲುರ್ಟಿ ಜಾತ್ರೆ, ಪಾರಂಕ ಮಹೋತ್ಸವ"),
            local("ಪಣಂಬೂರು ನಂದನೇಶ್ವರ ಧ್ವಜ"),
            local("ಬೇಕೂರು ಮುಕ್ಕಿಂಜ ಪೂಮಾಣಿ ಉತ್ಸವ"),
            local("ಮೂಡೆಬಿದಿರೆ ಬಡಗು ಬಸದಿ ರಥ"),
            local("ಸಿದ್ಧಾರ್ಥನಗರ ಕೋಟೆ ಬಬ್ಬುಸ್ವಾಮಿ ವಾರ್ಷಿಕ ನೇಮೋತ್ಸವ ಆರಂಭ"),
            local("ಬೋಳೂರು ಜಾರಂದಾಯ ಬಂಡಿ ಉತ್ಸವ"),
            civic("ಶ್ರೀ ವಿಶ್ವಕರ್ಮ ಜಯಂತಿ"),
            local("ಮೂಡೆಬಿದಿರೆ ವೆಂಕಟರಮಣ ಕುಂಭಾಭಿಷೇಕ ವರ್ಧಂತಿ"),
        ),
    )

    private val sunriseSunset = listOf(
        1 to (LocalTime.of(6, 54) to LocalTime.of(18, 14)),
        2 to (LocalTime.of(6, 54) to LocalTime.of(18, 14)),
        3 to (LocalTime.of(6, 55) to LocalTime.of(18, 15)),
        4 to (LocalTime.of(6, 55) to LocalTime.of(18, 16)),
        5 to (LocalTime.of(6, 55) to LocalTime.of(18, 16)),
        6 to (LocalTime.of(6, 56) to LocalTime.of(18, 17)),
        7 to (LocalTime.of(6, 56) to LocalTime.of(18, 17)),
        8 to (LocalTime.of(6, 56) to LocalTime.of(18, 18)),
        9 to (LocalTime.of(6, 57) to LocalTime.of(18, 19)),
        10 to (LocalTime.of(6, 57) to LocalTime.of(18, 19)),
        11 to (LocalTime.of(6, 57) to LocalTime.of(18, 20)),
        12 to (LocalTime.of(6, 57) to LocalTime.of(18, 20)),
        13 to (LocalTime.of(6, 58) to LocalTime.of(18, 21)),
        14 to (LocalTime.of(6, 58) to LocalTime.of(18, 21)),
        15 to (LocalTime.of(6, 58) to LocalTime.of(18, 22)),
        16 to (LocalTime.of(6, 58) to LocalTime.of(18, 22)),
        17 to (LocalTime.of(6, 58) to LocalTime.of(18, 23)),
        18 to (LocalTime.of(6, 58) to LocalTime.of(18, 23)),
        19 to (LocalTime.of(6, 58) to LocalTime.of(18, 24)),
        20 to (LocalTime.of(6, 58) to LocalTime.of(18, 24)),
        21 to (LocalTime.of(6, 59) to LocalTime.of(18, 25)),
        22 to (LocalTime.of(6, 59) to LocalTime.of(18, 25)),
        23 to (LocalTime.of(6, 59) to LocalTime.of(18, 26)),
        24 to (LocalTime.of(6, 59) to LocalTime.of(18, 26)),
        25 to (LocalTime.of(6, 59) to LocalTime.of(18, 27)),
        26 to (LocalTime.of(6, 59) to LocalTime.of(18, 27)),
        27 to (LocalTime.of(6, 59) to LocalTime.of(18, 28)),
        28 to (LocalTime.of(6, 59) to LocalTime.of(18, 29)),
        29 to (LocalTime.of(6, 59) to LocalTime.of(18, 29)),
        30 to (LocalTime.of(6, 58) to LocalTime.of(18, 29)),
        31 to (LocalTime.of(6, 58) to LocalTime.of(18, 30)),
    ).toMap()

    val days: List<LocalCalendarDay> = (1..month.lengthOfMonth()).map { day ->
        val times = checkNotNull(sunriseSunset[day])
        LocalCalendarDay(
            date = month.atDay(day),
            sunrise = times.first,
            sunset = times.second,
            observances = observancesByDay[day].orEmpty(),
        )
    }

    fun day(dayOfMonth: Int): LocalCalendarDay =
        requireNotNull(days.getOrNull(dayOfMonth - 1)) { "No January 2026 day $dayOfMonth" }
}
