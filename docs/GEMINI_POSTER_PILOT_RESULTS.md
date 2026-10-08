# Gemini poster pilot — 2026-10-08

## Scope and method

One fresh Google AI Studio playground run with Gemini 3.1 Flash Lite, Minimal thinking, web search disabled, and all six original user-supplied attachments. No API key selected; no paid billing enabled by this work. The user approved Google's upload acknowledgement. AI Studio indicated uploaded files are saved to Google Drive even in temporary chats.

The model returned valid JSON with six event objects. The page displayed 8.2 seconds for the response (not a measured Android/network latency guarantee). This playground run was a small pilot, not an accuracy benchmark or a production API test. At this stage the Poi APK/backend had not been changed to call Gemini; the later deployed-API follow-up is recorded below.

## Observed output versus manual review

| Image | English display name returned | Dates returned | Venue / locality returned | Review |
| --- | --- | --- | --- | --- |
| 1 | Gurupura Kambala | April 4, 5; year null | Manibettu guthu / Gurupura | Main name, footer location and days agree. Model calls dates `listed`; whether the event runs continuously still needs organizer/user confirmation. |
| 2 | IPL FAN PARK | May 29, 31; year null | Karavali Utsav Ground / Mangalore | Two separate dates correctly retained; no May 30 inserted. |
| 3 | SAVAARI LIVE CONCERT | April 19, 2026 | Kodi Beach / Kundapura | English display name and date agree, but `titleOriginal` was shortened to SAVAARI and its translation flag was incorrectly true. |
| 4 | Bantwala Kambala | March 7, 8, 2026 | Navoor / Bantwala | Main English name, dates and footer agree. The inner Kannada heading returned as `ಮೂಡೂರು - ಪದೂರು ಕಂಬಳ` appears misread and requires Kannada-speaker review. |
| 5 | Rambo Circus | August 22, 23, 26, 27, 28, 29, 30; September 4, 5, 6; year null | Dr TMA Pai Intl. Convention Center, MG Road / Mangaluru | All ten dates and 17:30 / 20:00 shows agree. `evidence.time` was normalized to 24-hour time rather than copied literally from the poster. |
| 6 | Dharma Nema | April 23–29, 2022, expanded to seven days with `range` relationship | Varkadi Chavadibailu Guthu Gadupadi Mane / locality null | Title and historical year retained. Kannada venue transliteration requires human confirmation and is not independently verified. |

Checks performed on the actual returned JSON: six objects present; absent years remain null for images 1, 2 and 5; all end times null; circus show starts exactly 17:30 and 20:00; every date for image 6 retains year 2022. Fully dated 2026 examples were classified past as of the supplied review date; undated years were classified unknown.

No claims are made about Hindi, blurry images, unseen posters, repeated-run consistency, or native-app OCR improvement from this result. The comparison does not establish numerical improvement over Tesseract because matched native OCR output was not collected for these files.

## Recommendation before deployment

Proceed with a controlled cloud-reader beta, not unattended event publication. Prefer the readable literal summary heading where available, separate original script from English display text, and require review for original-script/title disagreements. Enforce JSON/date/time validation in backend code; never trust self-reported confidence or silently replace a missing year. Represent separate show dates and continuous ranges separately. Show the poster alongside draft fields and retain manual editing plus the offline fallback.

Use a server-held Gemini API key, authenticated requests, request/image size limits, per-user and global usage caps, an explicit cloud-upload notice, and friendly unavailable/quota errors. Do not embed the key in the APK or commit it to Git. Backend setup and a real API test are still required before changing the production APK. A recurring/bulk event publishing feature is not part of this pilot.
# Deployed API follow-up (8 October 2026)

After the playground pilot, the authenticated production Edge Function was tested on six JPEG-encoded supplied posters using two temporary identities. All six passed the limited automated date/time assertions: absent years remained null (1, 2, 5); IPL retained only May 29/31; Savaari retained 19 April 2026; Circus returned all ten dates and 17:30/20:00; Dharma Nema retained a 23–29 April 2022 range. All omitted end times stayed null. Returned titles were Gurupura Kambala, IPL FAN PARK, SAVAARI LIVE CONCERT, Bantwala Kambala, Rambo Circus and ಧರ್ಮ ನೇಮ. These checks are not a universal accuracy score or a Hindi evaluation. Original Kannada names and venue spellings still require human review.

Unauthenticated function requests and direct authenticated-user access to quota reservation were rejected. Both temporary identities were removed; no test events were published. Full JSON is kept locally in ignored artifacts, not the public repository. No paid billing was enabled.
