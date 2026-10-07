# Poster reader — first field-testing release

Create → Choose poster → choose language if needed → Read poster → compare recognized text → Use this draft → confirm exact start/end, name and location → Publish for review.

The reader uses bundled Kannada, English and Hindi neural OCR (Tesseract LSTM), then conservative local rules to structure a draft. It is not a cloud vision-language model or a newly trained proprietary model. No API key, outbound poster upload, or per-scan charge. Models add about 8.8 MB before APK compression; native runtime also increases package size.

Limits: decorative lettering, blurred/low-resolution images, complicated columns, unusual date formats, and multiple programmes may require manual correction. Name/category and unlabelled footer locations are suggestions. Partial dates and advertised date lists are offered as selectable suggestions: confirm omitted years, choose an occurrence, or explicitly confirm a detected range. Multiple show times are selectable. Unknown duration remains for manual review. No automatic translation, organizer verification, geocoding, or poster attachment is performed. Never claim 100% accuracy across all scripts.

The layout-aware reader removes low-confidence OCR words and prioritizes larger text rather than the first watermark. It enlarges small images and uses grayscale/contrast normalization, including inversion of predominantly dark posters. Automatic mode also runs an English-only pass for recognizable English event headers to reduce competing-script errors. These are heuristics, not a vision-language model; they cannot recover information absent from a tiny or blurred image. The recognition budget remains 60 seconds; a failed optional second pass preserves the initial usable scan.

## Supplied poster examples: human-reviewed target drafts

These are visual interpretations for comparison, NOT measured OCR output or newly published events. The regression tests use human transcriptions to test the parser. Native Android recognition still requires phone testing against the original images.

| Event | Advertised dates | Venue / locality | Review needed |
| --- | --- | --- | --- |
| Gurupura Kambala | April 4 and 5; year absent | Manibettu guthu, Gurupura | Confirm year and whether this is one continuous event; time absent |
| IPL Fan Park | May 29 and 31; year absent | Karavali Utsav Ground, Mangalore | Two separate occurrences, not May 29–31 continuously; times absent |
| Savaari Live Concert | April 19, 2026 | Kodi Beach, Kundapura | Start/end times absent |
| Bantwala Kambala | March 7–8; 2026 appears in the inner poster | Navoor, Bantwala | Confirm small inner date/year and start/end times |
| Rambo Circus / ರಾಂಬೋ ಸರ್ಕಸ್ | August 22, 23, 26, 27, 28, 29, 30; September 4, 5, 6; year absent | Dr TMA Pai International Convention Centre, MG Road, Mangaluru | Select one occurrence and confirm year; two daily starts: 17:30 and 20:00; end times absent |
| ಧರ್ಮ ನೇಮ (Dharma Nema) | April 23–29, 2022 | Local family/house information needs careful Kannada review | Historical poster; no invented venue or upcoming year; time absent |

No organizer, exact coordinate, ticket price or end time can be invented from these examples. Separate dates/shows currently become separate event drafts; bulk recurrence publishing is not implemented. No fixtures are seeded into the production database.

Images stay on the device and are not retained by Poi after scanning; models live in app-private storage. Sharing recognized text for feedback is an explicit user action and may include contact information from the poster. Publishing transmits the final reviewed event details to the existing Poi backend.

Test 5+ posters per group: Kannada, Kannada+English, Hindi+English, English. Include sharp vs blurry, date with no year, multiple dates, timetable, numeric/Indic digits, landscape, and sideways posters (Rotate 90° before reading). Record wrong/missing name, date, time, venue and category separately; share the original and recognized text only if you have permission. Check end-before-start is blocked, and check the published event's schedule against the original. Cancelled reads must not overwrite forms; choosing another draft resets confirmation.

Parser unit tests exercise Kannada/Hindi digits, month names, time markers, numeric dates, missing years, ambiguous times, invalid dates and schedule parsing. Android compilation/lint and a signed build do not establish actual OCR accuracy. Real-device testing is required; production accuracy has not been benchmarked.

Models: official https://github.com/tesseract-ocr/tessdata_fast at revision 87416418657359cb625c412a48b6e1d6d41c29bd, Apache-2.0. Android runtime: https://github.com/adaptech-cz/Tesseract4Android, version 4.9.0. License/provenance bundled in poster-ocr assets.
