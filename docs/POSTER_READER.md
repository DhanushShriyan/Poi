# Poster reader — first field-testing release

Create → Choose poster → choose language if needed → Read poster → compare recognized text → Use this draft → confirm exact start/end, name and location → Publish for review.

The reader uses bundled Kannada, English and Hindi neural OCR (Tesseract LSTM), then conservative local rules to structure a draft. It is not a cloud vision-language model or a newly trained proprietary model. No API key, outbound poster upload, or per-scan charge. Models add about 8.8 MB before APK compression; native runtime also increases package size.

Limits: decorative lettering, blurred/low-resolution images, complicated columns, unusual date formats, and multiple programmes may require manual correction. Name/category are suggestions. Location extraction currently requires an explicit venue/address label with a separator. Dates without a year, ambiguous 12-hour times, multiple dates/times, and unknown duration remain for manual review. No automatic translation, organizer verification, geocoding, or poster attachment is performed. Never claim 100% accuracy across all scripts.

Images stay on the device and are not retained by Poi after scanning; models live in app-private storage. Sharing recognized text for feedback is an explicit user action and may include contact information from the poster. Publishing transmits the final reviewed event details to the existing Poi backend.

Test 5+ posters per group: Kannada, Kannada+English, Hindi+English, English. Include sharp vs blurry, date with no year, multiple dates, timetable, numeric/Indic digits, landscape, and sideways posters (Rotate 90° before reading). Record wrong/missing name, date, time, venue and category separately; share the original and recognized text only if you have permission. Check end-before-start is blocked, and check the published event's schedule against the original. Cancelled reads must not overwrite forms; choosing another draft resets confirmation.

Parser unit tests exercise Kannada/Hindi digits, month names, time markers, numeric dates, missing years, ambiguous times, invalid dates and schedule parsing. Android compilation/lint and a signed build do not establish actual OCR accuracy. Real-device testing is required; production accuracy has not been benchmarked.

Models: official https://github.com/tesseract-ocr/tessdata_fast at revision 87416418657359cb625c412a48b6e1d6d41c29bd, Apache-2.0. Android runtime: https://github.com/adaptech-cz/Tesseract4Android, version 4.9.0. License/provenance bundled in poster-ocr assets.
