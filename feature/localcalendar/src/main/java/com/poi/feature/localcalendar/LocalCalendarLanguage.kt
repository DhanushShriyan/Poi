package com.poi.feature.localcalendar

import java.util.Locale

enum class LocalCalendarLanguage(val storageValue: String) {
    ENGLISH("en"),
    KANNADA("kn");

    val locale: Locale
        get() = when (this) {
            ENGLISH -> Locale.ENGLISH
            KANNADA -> Locale.Builder().setLanguage("kn").setRegion("IN").build()
        }

    val strings: LocalCalendarStrings
        get() = when (this) {
            ENGLISH -> EnglishLocalCalendarStrings
            KANNADA -> KannadaLocalCalendarStrings
        }

    companion object {
        val DEFAULT = ENGLISH

        fun fromStorage(value: String?): LocalCalendarLanguage =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}

data class LocalCalendarStrings(
    val screenTitle: String,
    val back: String,
    val monthTitle: String,
    val dateHint: String,
    val weekdays: List<String>,
    val local: String,
    val festival: String,
    val civic: String,
    val noObservances: String,
    val emptyDayHint: String,
    val localObservances: String,
    val showLess: String,
    val sunrise: String,
    val sunset: String,
    val source: String,
    val featured: String,
) {
    fun observanceCount(count: Int): String = when (this) {
        EnglishLocalCalendarStrings -> "$count local observance${if (count == 1) "" else "s"}"
        else -> "$count ಸ್ಥಳೀಯ ಆಚರಣೆಗಳು"
    }

    fun viewAll(count: Int): String = when (this) {
        EnglishLocalCalendarStrings -> "View all $count"
        else -> "ಎಲ್ಲ $count ನೋಡಿ"
    }

    fun showMore(count: Int): String = when (this) {
        EnglishLocalCalendarStrings -> "Show $count more"
        else -> "ಇನ್ನೂ $count ತೋರಿಸಿ"
    }
}

private val EnglishLocalCalendarStrings = LocalCalendarStrings(
    screenTitle = "Local calendar",
    back = "Back",
    monthTitle = "January 2026",
    dateHint = "Tap a date to see a quiet preview.",
    weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
    local = "Local",
    festival = "Festival",
    civic = "Civic",
    noObservances = "No listed observances",
    emptyDayHint = "Sunrise and sunset information is still available for this date.",
    localObservances = "LOCAL OBSERVANCES",
    showLess = "Show less",
    sunrise = "Sunrise",
    sunset = "Sunset",
    source = "Source: Sharada Calendar 2026 · Sunrise and sunset for Mangaluru",
    featured = "FEATURED",
)

private val KannadaLocalCalendarStrings = LocalCalendarStrings(
    screenTitle = "ಸ್ಥಳೀಯ ಕ್ಯಾಲೆಂಡರ್",
    back = "ಹಿಂದೆ",
    monthTitle = "ಜನವರಿ 2026",
    dateHint = "ವಿವರಗಳನ್ನು ನೋಡಲು ದಿನಾಂಕವನ್ನು ಆಯ್ಕೆಮಾಡಿ.",
    weekdays = listOf("ಭಾನು", "ಸೋಮ", "ಮಂಗಳ", "ಬುಧ", "ಗುರು", "ಶುಕ್ರ", "ಶನಿ"),
    local = "ಸ್ಥಳೀಯ",
    festival = "ಹಬ್ಬ",
    civic = "ರಾಷ್ಟ್ರೀಯ",
    noObservances = "ಯಾವುದೇ ಆಚರಣೆ ಪಟ್ಟಿ ಮಾಡಿಲ್ಲ",
    emptyDayHint = "ಈ ದಿನದ ಸೂರ್ಯೋದಯ ಮತ್ತು ಸೂರ್ಯಾಸ್ತದ ಮಾಹಿತಿ ಲಭ್ಯವಿದೆ.",
    localObservances = "ಸ್ಥಳೀಯ ಆಚರಣೆಗಳು",
    showLess = "ಕಡಿಮೆ ತೋರಿಸಿ",
    sunrise = "ಸೂರ್ಯೋದಯ",
    sunset = "ಸೂರ್ಯಾಸ್ತ",
    source = "ಮೂಲ: ಶಾರದಾ ಕ್ಯಾಲೆಂಡರ್ 2026 · ಮಂಗಳೂರಿನ ಸೂರ್ಯೋದಯ ಮತ್ತು ಸೂರ್ಯಾಸ್ತ",
    featured = "ಪ್ರಮುಖ",
)

fun LocalObservance.displayTitle(language: LocalCalendarLanguage): String =
    if (language == LocalCalendarLanguage.KANNADA) title else EnglishObservanceTitles[title] ?: title

/**
 * English display copy for the exact Kannada source records in [January2026Calendar].
 * The original Kannada transcription remains untouched and is always available to the user.
 */
private val EnglishObservanceTitles = mapOf(
    "ಪ್ರದೋಷ, ಪದ್ಯಾಣ ಮಹಾಲಿಂಗೇಶ್ವರ ಉತ್ಸವ" to "Pradosha; Padyana Mahalingeshwara Festival",
    "ಕ್ರಿಸ್ತಶಕ 2026 ಆರಂಭ, ತಡಗಜಿ ದೈವ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ" to "Gregorian year 2026 begins; Tadagaji Daiva consecration anniversary",
    "ಮಾಯಿಪ್ಪಾಡಿ ವಿಷ್ಣುಮೂರ್ತಿ ಜಾತ್ರಾರಂಭ" to "Mayippady Vishnumurthy festival begins",
    "ಮಾಯಿಪ್ಪಾಡಿ ವಿಷ್ಣುಮೂರ್ತಿ ಒತ್ತೆಕೋಲ" to "Mayippady Vishnumurthy Ottakola",
    "ಕೊಲ್ಲೂರು ಮಂಡಲಪೂಜೆ, ಶಾಕಂಬರಿ ಚತುರ್ಥಿ" to "Kollur Mandala Pooja; Shakambari Chaturthi",
    "ಮಣ್ಗಡ್ಡೆ ನವದುರ್ಗಾಗಣಪತಿ ದೇವಳದಲ್ಲಿ ಚಂಡಿಕಾಯಾಗ" to "Chandika Yaga at Mangadde Navadurga Ganapathi Temple",
    "ಪೌರ್ಣಮಿ, ಪದ್ಯಾಣ ಧೂಮಾವತಿ ನೇಮೋತ್ಸವ" to "Pournami; Padyana Dhoomavathi Nemotsava",
    "ಎರ್ಮಾರ್ ಮಂಡಲಪೂಜೆ, ಕುನಾರು ಅಮ್ಮಂಬಳ ಸೋಮನಾಥೇಶ್ವರ ಮಂಡಲಪೂಜೆ" to "Ermar Mandala Pooja; Kunaru Ammambala Somanatheshwara Mandala Pooja",
    "ಬಜ್ಪೆ ಶನೈಶ್ಚರ ದೇವರ ವರ್ಧಂತ್ಯುತ್ಸವ" to "Bajpe Shanaishchara Deva anniversary festival",
    "ಉಳ್ಳಾಲ ಉಳ್ಳಾಲ್ತಿ ಧರ್ಮ ಅರಸರ ಶ್ರೀ ದುರ್ಗಾಹೋಮ" to "Sri Durga Homa of Ullal Ullalthi Dharma Arasu",
    "ಕೇರಿಂಜ ಉಳ್ಳಾಲ್ತಿ ನವೀಕರಣ ವಾರ್ಷಿಕೋತ್ಸವ" to "Kerinje Ullalthi renovation anniversary",
    "ಕೊಡವೂರು ಶಂಕರನಾರಾಯಣ ರಥ" to "Kodavoor Shankaranarayana Rathotsava",
    "ಕಂಕನಾಡಿ ಬ್ರಹ್ಮಬೈದರ್ಕಳ ಜಾತ್ರೆ" to "Kankanady Brahma Baidarkala Jatre",
    "ಉಳ್ಳಾಲ ಉಳ್ಳಾಲ್ತಿ ಧರ್ಮ ಅರಸರ ಗಣಪತಿ ಹೋಮ" to "Ganapathi Homa of Ullal Ullalthi Dharma Arasu",
    "ಸಂಕಷ್ಟ ಚತುರ್ಥಿ – ಚಂ.ಉ.ಫಂ. 9–20" to "Sankashta Chaturthi · moonrise 9:20 PM",
    "ಮದಗ (ಕುಂಭಾರು) ಜನಾರ್ದನ ರಥ" to "Madaga (Kumbaru) Janardana Rathotsava",
    "ಉಡುಪಿ ಸಪ್ತೋತ್ಸವಾರಂಭ, ಕೋಟೆ ಅಮ್ಮತೇರು ಜಾತ್ರೆ" to "Udupi Saptotsava begins; Kote Ammateru Jatre",
    "ಉಳ್ಳಾಲ ಉಳ್ಳಾಲ್ತಿ ಧರ್ಮ ಅರಸರ ಚಂಡಿಕಾಯಾಗ" to "Chandika Yaga of Ullal Ullalthi Dharma Arasu",
    "ಸುಳ್ಯ ಚೆನ್ನಕೇಶವ ರಥ" to "Sullia Chennakeshava Rathotsava",
    "ಶ್ರೀ ವೀರವೆಂಕಟೇಶ ಪುನಃಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ, ಮಹಾನಕ್ಷತ್ರ ಉತ್ತರಾಭಾದ್ರ ಪ್ರಾರಂಭ" to "Sri Veera Venkatesha reconsecration anniversary; Mahanakshatra Uttarabhadra begins",
    "ಸ್ವಾಮಿ ವಿವೇಕಾನಂದ ಜನ್ಮದಿನ" to "Swami Vivekananda Jayanti",
    "ಮಕರಸಂಕ್ರಮಣ" to "Makara Sankramana",
    "ಸರ್ವೈಕಾದಶಿ, ಮಕರಸಂಕ್ರಮಣ, ಕರಿ ತೀರ್ಥ, ಪೊಂಗಲ್, ಕುಂಬಳೆ ಧ್ವಜ" to "Sarva Ekadashi; Makara Sankramana; Kari Theertha; Pongal; Kumble Dhwaja",
    "ಸಜ್ಜಪಜಾತ್ರೆ, ಉಡುಪಿ ಮೂರು ರಥ, ಕಾವೂರು ಮಹಾಲಿಂಗೇಶ್ವರ ಧ್ವಜ" to "Sajjapa Jatre; Udupi Mooru Ratha; Kavoor Mahalingeshwara Dhwaja",
    "ಕೊಡ್ಲು ವಿಷ್ಣುಮಂಗಲ ಮಹಾವಿಷ್ಣು ಜಾತ್ರೆ" to "Kodlu Vishnumangala Mahavishnu Jatre",
    "ಸಂಗಡಿಬಿಟ್ಟು ಬನಶಂಕರಿ ಉತ್ಸವ, ಬಜವಳ್ಳಿ ಅಯ್ಯಪ್ಪ ಮಹಾಭಿಷೇಕ" to "Sangadibittu Banashankari festival; Bajavalli Ayyappa Mahabhisheka",
    "ಶ್ರೀ ಕ್ಷೇತ್ರ ಒಂಬತ್ತು ಕೆರೆ ಚಂಡಿಕಾಯಾಗ" to "Chandika Yaga at Sri Kshetra Ombattu Kere",
    "ಕಾವೂರು ಮುಳ್ಳಕಾಡು ಅಯ್ಯಪ್ಪ ದೀಪೋತ್ಸವ" to "Kavoor Mullakadu Ayyappa Deepotsava",
    "ಕೋಡಿಕಲ್ ಜಿ.ಎಸ್.ಬಿ. ಸಾಮೂಹಿಕ ಸತ್ಯನಾರಾಯಣ ಪೂಜೆ" to "Kodial GSB community Satyanarayana Pooja",
    "ನಂದಾವರ ಶಂಕರನಾರಾಯಣ ಸತ್ಯನಾರಾಯಣ ಪೂಜೆ" to "Nandavara Shankaranarayana Satyanarayana Pooja",
    "ಇರುವೈಲು ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಭಜನಾ ಮಂಗಲ" to "Iruvailu Durgaparameshwari Bhajana Mangala",
    "ಕುಡ್ರೋಳಿ ಭಗವತಿ ಚಂಡಿಕಾಯಾಗ" to "Kudroli Bhagavathi Chandika Yaga",
    "ಮಂಗಳೂರು ಮುನೀಶ್ವರ ಮಹಾಗಣಪತಿ ವಾರ್ಷಿಕೋತ್ಸವ" to "Mangaluru Muneeshwara Mahaganapathi anniversary festival",
    "ಉಡುಪಿ ಚೂರ್ಣೋತ್ಸವ, ಕುಂಬಳೆ ಪುತಿಗೆ ಸುಬ್ರಾಯ ಮಹೋತ್ಸವ" to "Udupi Churnotsava; Kumble Puthige Subraya Mahotsava",
    "ಕುಂದಾಪುರ ಅರಸಮ್ಮಕಾನ ಶ್ರೀದೇವಿ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಕೊಂಡೋತ್ಸವ" to "Kundapura Arasammakana Sridevi Durgaparameshwari Kondotsava",
    "ಮಂಜನಾಡಿ ವಿಷ್ಣುಮೂರ್ತಿ ಜನಾರ್ದನ ಧ್ವಜ" to "Manjanady Vishnumurthy Janardana Dhwaja",
    "ಮಹಾಗಣಪತಿ ದೇವಸ್ಥಾನ ಉರ್ವಸ್ಟೋರ್ ನವಗ್ರಹ ಯಾಗ" to "Navagraha Yaga at Urwa Store Mahaganapathi Temple",
    "ದೇಂತಡ್ಕ ವನದುರ್ಗಾ ಧ್ವಜ, ಕೆಮ್ಮಾಯಿ ಓಂ ಅಯ್ಯಪ್ಪ ವರ್ಧಂತಿ" to "Dentadka Vanadurga Dhwaja; Kemmai Om Ayyappa anniversary",
    "ಶಂಕರನಾರಾಯಣ–ಕುಂಭಾಶಿ–ಸಾಲಿಗ್ರಾಮ–ಧಾರೇಶ್ವರ ರಥ" to "Shankaranarayana–Kumbhashi–Saligrama–Dhareshwara Rathotsavas",
    "ಮಾಸಶಿವರಾತ್ರಿ, ಕಣ್ಣೂರು ಮಹೋತ್ಸವ, ಕುಂಬಳೆ ಬೆಡಿ ಉತ್ಸವ" to "Masa Shivaratri; Kannur Mahotsava; Kumble Bedi festival",
    "ದೇಂತಡ್ಕ ವನದುರ್ಗಾ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ, ಕಾವೂರು ಮಹಾಲಿಂಗೇಶ್ವರ ರಥ" to "Dentadka Vanadurga consecration anniversary; Kavoor Mahalingeshwara Rathotsava",
    "ನಾವೂರು ಕೊಡಿಬೈಲು ಮಹಮ್ಮಾಯಿ ಜಾತ್ರೆ" to "Navoor Kodibailu Mahammai Jatre",
    "ಕಟೀಲು ಮಾಲಿದೇವಿ ವಾರ್ಷಿಕ ಉತ್ಸವ ಆರಂಭ" to "Kateel Malidevi annual festival begins",
    "ಅಮಾವಾಸ್ಯೆ, ಪುರಂದರದಾಸರ ಪುಣ್ಯದಿನ, ದೇಂತಡ್ಕ ವನದುರ್ಗಾ ರಥ" to "Amavasya; Purandara Dasa Punyadina; Dentadka Vanadurga Rathotsava",
    "ಮಾಘಸ್ನಾನಾರಂಭ, ಮಂಜನಾಡಿ ವಿಷ್ಣುಮೂರ್ತಿ ಜನಾರ್ದನ ಉತ್ಸವ" to "Magha Snana begins; Manjanady Vishnumurthy Janardana festival",
    "ಉಡುಪಿ ಶಿರೂರು ಪರ್ಯಾಯೋತ್ಸವ, ಪಂಚಗ್ರಹ ಯೋಗ" to "Udupi Shirur Paryaya festival; Panchagraha Yoga",
    "ದೇಂತಡ್ಕ ವನದುರ್ಗಾ ಅವಭೃತ, ಕಾನಂಗಿ ಶ್ರೀನಿವಾಸ ಧ್ವಜ" to "Dentadka Vanadurga Avabhrutha; Kanangi Srinivasa Dhwaja",
    "ಕಮಾರಡು ಮಹಾವಿಷ್ಣು ಮಹೋತ್ಸವ" to "Kamaradu Mahavishnu Mahotsava",
    "ಸುರತ್ಕಲ್ಲು ವೀರಭದ್ರ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಧ್ವಜ" to "Surathkal Veerabhadra Durgaparameshwari Dhwaja",
    "ಮೀಂಜ ಕೊರಿಕ್ಕಾರು ವಿಷ್ಣುಮೂರ್ತಿ ಮಯ್ಯನಾಡು ದೈವ ಉತ್ಸವ" to "Meenja Korikkaru Vishnumurthy Mayyanadu Daiva festival",
    "ಮಂಗಳೂರು ಶ್ರೀ ವೀರವೆಂಕಟೇಶ ದೇವರಿಗೆ ಸಹಸ್ರ ಕುಂಭಾಭಿಷೇಕ" to "Sahasra Kumbhabhisheka for Sri Veera Venkatesha, Mangaluru",
    "ಚಂದ್ರದರ್ಶನ, ಮಂಗಳೂರು ಲಕ್ಷ್ಮೀ ನಾರಾಯಣ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ" to "Chandra Darshana; Mangaluru Lakshmi Narayana consecration anniversary",
    "ಉಜಿರೆ ಜನಾರ್ದನ ರಥ, ವಿಟ್ಲ ಪಂಚಲಿಂಗೇಶ್ವರ ರಥ" to "Ujire Janardana Rathotsava; Vittla Panchalingeshwara Rathotsava",
    "ಕದ್ರಿ ರಥ, ಬಲಾಡು ವನಶಾಸ್ತಾರ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ" to "Kadri Rathotsava; Baladu Vanashasthara consecration anniversary",
    "ಉಪ್ಪಳ ಸಂತಡ ಅರಸು ಸಂಕಲ ದೈವ ಉತ್ಸವ" to "Uppala Santhada Arasu Sankala Daiva festival",
    "ಕಚ್ಚೂರು ಮಾಲತಿದೇವಿ ಬಬ್ಬುಸ್ವಾಮಿ ಕೊಂಡಸೇವೆ" to "Kachur Malathidevi Babbuswami Konda Seve",
    "ವಿನಾಯಕ ಚತುರ್ಥಿ, ಸೌತಡ್ಕ ಮಹಾಗಣಪತಿ ಮೂಡಪ್ಪ ಸೇವೆ" to "Vinayaka Chaturthi; Sauthadka Mahaganapathi Moodappa Seve",
    "ಕಟೀಲು ಮಹೋತ್ಸವ, ಮಹಾಗಣಪತಿ ದೇವಸ್ಥಾನ ಉರ್ವಸ್ಟೋರ್ ಮಂಗಳೂರು ವಾರ್ಷಿಕ ಜಾತ್ರೆ ಆರಂಭ" to "Kateel Mahotsava; Urwa Store Mahaganapathi Temple annual jatre begins",
    "ವಸಂತ ಪಂಚಮಿ, ಸುರತ್ಕಲ್ಲು ಪುರಾತನ ಮಾರಿಯಮ್ಮ ಚೂರ್ಣೋತ್ಸವ" to "Vasantha Panchami; Surathkal Purathana Mariyamma Churnotsava",
    "ಸುರತ್ಕಲ್ಲು ವೀರಭದ್ರ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಚೂರ್ಣೋತ್ಸವ" to "Surathkal Veerabhadra Durgaparameshwari Churnotsava",
    "ಬಸ್ರೂರು ಮಹಾಲಿಂಗ ನಾರಾಯಣಿ ರಥ, ಅಗಲಾಡಿ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಧ್ವಜ" to "Basrur Mahalinga Narayani Rathotsava; Agaladi Durgaparameshwari Dhwaja",
    "ಕಾಂಞಂಗಾಡ್ ಲಕ್ಷ್ಮೀ ವೆಂಕಟೇಶ ಭಜನಾ ಸಪ್ತಾಹಾರಂಭ" to "Kanhangad Lakshmi Venkatesha Bhajana Saptaha begins",
    "ಶಿರಾಡಿ ಜಾತ್ರೆ, ಕುಂಗ್ಯಾ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಉತ್ಸವ" to "Shiradi Jatre; Kungya Durgaparameshwari festival",
    "ಮಹಾನಕ್ಷತ್ರ ಶ್ರವಣ ಪ್ರಾರಂಭ" to "Mahanakshatra Shravana begins",
    "ಕವಿ ಮುದ್ದಣ್ಣ ಜಯಂತಿ, ಕೊಡದಡ ಅನ್ನಪೂರ್ಣೆ ಉತ್ಸವಾರಂಭ" to "Kavi Muddanna Jayanti; Kodadada Annapoorne festival begins",
    "ಕೊಡದಡ ಮಲರಾಯ ನೇಮೋತ್ಸವ, ಕಾರ್ಕಳ ದೇವಕೃಷ್ಣ ವರ್ಧಂತಿ" to "Kodadada Malaraya Nemotsava; Karkala Devakrishna anniversary",
    "ರಥಸಪ್ತಮಿ, ಮಂಗಳೂರು–ಕುಮಟಾ–ವೆಂಕಾಪುರ ರಥ" to "Ratha Saptami; Mangaluru–Kumta–Venkapura Rathotsavas",
    "ಮರೋಳಿ ಸೂರ್ಯನಾರಾಯಣ ರಥ" to "Maroli Suryanarayana Rathotsava",
    "ಕಾನಂಗಿ ಶ್ರೀನಿವಾಸ ರಥ, ದುರ್ಗಾ ಹರಿಹರೇಶ್ವರ ರಥ" to "Kanangi Srinivasa Rathotsava; Durga Harihareshwara Rathotsava",
    "ಪೋಳ್ಯ ಲಕ್ಷ್ಮೀ ವೆಂಕಟರಮಣ ರಥ, ಪಣಂಬೂರು ವಿಷ್ಣುಮೂರ್ತಿ ರಥ" to "Polya Lakshmi Venkataramana Rathotsava; Panambur Vishnumurthy Rathotsava",
    "ಕೊಲ್ಲಮೊಗರು ಕೊಚ್ಚಿಲ ಸುಬ್ರಹ್ಮಣ್ಯ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ" to "Kollamogaru Kochila Subrahmanya consecration anniversary",
    "ಕೊಡದಡ ಅನ್ನಪೂರ್ಣೆ ಕೆರೆ ಉತ್ಸವ" to "Kodadada Annapoorne Kere festival",
    "ಉರ್ವಸ್ಟೋರ್ ಮಹಾಗಣಪತಿ ಹಗಲು ರಥೋತ್ಸವ" to "Urwa Store Mahaganapathi day Rathotsava",
    "ಭೀಷ್ಮಾಷ್ಟಮಿ, ಕೊಡದಡ ಅನ್ನಪೂರ್ಣೆ ರಥ, ಇಡಗುಂಜಿ ವಿನಾಯಕ ರಥ" to "Bhishmashtami; Kodadada Annapoorne Rathotsava; Idagunji Vinayaka Rathotsava",
    "ಮಂಗಳೂರು ಪೊಲೀಸ್‌ಲೈನ್ ಮುನೀಶ್ವರ ಗಣಪತಿ ವಾರ್ಷಿಕೋತ್ಸವ" to "Mangaluru Police Line Muneeshwara Ganapathi annual festival",
    "ಗಣರಾಜ್ಯ ದಿನ" to "Republic Day",
    "ಮಲಾರಬೀಡು ಸಾಮೂಹಿಕ ಕಾಲಾವಧಿ ಸತ್ಯನಾರಾಯಣ ಪೂಜೆ, ಗಣರಾಜ್ಯ ದಿನ" to "Malarabidu community Kalavadhi Satyanarayana Pooja; Republic Day",
    "ಮದ್ದನವಮಿ, ನಾಳ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ರಥ" to "Maddanavami; Nala Durgaparameshwari Rathotsava",
    "ಅಗಲಾಡಿ ಮಹೋತ್ಸವ, ಮುಗೇರಡ್ಕ ಶಿರಾಡಿ ದೈವ ಜಾತ್ರೆ" to "Agaladi Mahotsava; Mugeradka Shiradi Daiva Jatre",
    "ಮೊಗಸನಾಡು ರಥ, ಕಟಪಾಡಿ ರಥ" to "Mogasanadu Rathotsava; Katapadi Rathotsava",
    "ಕುಂಗ್ಯಾ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಜಾತ್ರೆ" to "Kungya Durgaparameshwari Jatre",
    "ಉಪ್ಪಿನಂಗಡಿ ಲಕ್ಷ್ಮೀ ವೆಂಕಟರಮಣ ರಥ" to "Uppinangady Lakshmi Venkataramana Rathotsava",
    "ಅಗಲಾಡಿ ದುರ್ಗಾಪರಮೇಶ್ವರಿ ಅವಭೃತ" to "Agaladi Durgaparameshwari Avabhrutha",
    "ಒಡಿಯೂರು ವಾರ್ಷಿಕೋತ್ಸವ ರಥ" to "Odiyoor anniversary Rathotsava",
    "ಕಾನಂಗಿ ಗಾಯತ್ರಿದೇವಿ ಪ್ರತಿಷ್ಠಾ ವರ್ಧಂತಿ" to "Kanangi Gayathridevi consecration anniversary",
    "ಸುಕ್ಷೇತ್ರ ತಿಂಥಿಣಿ ಮೌನೇಶ್ವರ ಉತ್ಸವಾರಂಭ" to "Sukshetra Tinthini Mouneshwara festival begins",
    "ಸರ್ವೈಕಾದಶಿ, ನಾರಂಪಾಡಿ ಉಮ್ಮಮಹೇಶ್ವರ ಧ್ವಜ" to "Sarva Ekadashi; Narampady Ummamaheshwara Dhwaja",
    "ಪ್ರದೋಷ, ಶ್ರೀ ವಾದಿರಾಜ ಜಯಂತಿ" to "Pradosha; Sri Vadiraja Jayanti",
    "ಬಲಾಡು ಬಟ್ಟೆ ವಿನಾಯಕ ಉತ್ಸವ" to "Baladu Batte Vinayaka festival",
    "ಬೇಕೂರು ಮುಕ್ಕಿಂಜ ಕಿನ್ನಿಮಾಣಿ ಉತ್ಸವ" to "Bekur Mukkinja Kinnimani festival",
    "ಬೊಳೂರು ಜಾರಂದಾಯ ಬಂಡಿ ಉತ್ಸವ ಧ್ವಜಾರೋಹಣ" to "Bolur Jarandaya Bandi festival flag-hoisting",
    "ಕಳಸ ರಥ, ಕಾಂಞಂಗಾಡ್ ಭಜನಾ ಸಪ್ತಾಹ ಮಂಗಲ" to "Kalasa Rathotsava; Kanhangad Bhajana Saptaha Mangala",
    "ಕಲ್ಲೇಗ ಕಲ್ಲುರ್ಟಿ ಜಾತ್ರೆ, ಪಾರಂಕ ಮಹೋತ್ಸವ" to "Kallega Kallurti Jatre; Paranka Mahotsava",
    "ಪಣಂಬೂರು ನಂದನೇಶ್ವರ ಧ್ವಜ" to "Panambur Nandaneshwara Dhwaja",
    "ಬೇಕೂರು ಮುಕ್ಕಿಂಜ ಪೂಮಾಣಿ ಉತ್ಸವ" to "Bekur Mukkinja Poomani festival",
    "ಮೂಡೆಬಿದಿರೆ ಬಡಗು ಬಸದಿ ರಥ" to "Moodbidri Badagu Basadi Rathotsava",
    "ಸಿದ್ಧಾರ್ಥನಗರ ಕೋಟೆ ಬಬ್ಬುಸ್ವಾಮಿ ವಾರ್ಷಿಕ ನೇಮೋತ್ಸವ ಆರಂಭ" to "Siddharthanagara Kote Babbuswami annual Nemotsava begins",
    "ಬೋಳೂರು ಜಾರಂದಾಯ ಬಂಡಿ ಉತ್ಸವ" to "Bolur Jarandaya Bandi festival",
    "ಶ್ರೀ ವಿಶ್ವಕರ್ಮ ಜಯಂತಿ" to "Sri Vishwakarma Jayanti",
    "ಮೂಡೆಬಿದಿರೆ ವೆಂಕಟರಮಣ ಕುಂಭಾಭಿಷೇಕ ವರ್ಧಂತಿ" to "Moodbidri Venkataramana Kumbhabhisheka anniversary",
)
