# Security

Do not open a public issue containing personal data, service credentials, signing material, or a reproducible exploit against a live user.

For this test project, report security concerns to `mjshriyan8@gmail.com` with the subject `Poi security`.

Never commit `google-services.json`, service account files, `.env` secrets, Supabase service-role keys, or Android signing keys.

Poi update APKs must be signed by the certificate whose SHA-256 digest is `1ccdc26537c7d22ae8158f43e797c105c19927455a51f9dc9ec902148674fadc`. Treat `.signing/poi-update.jks`, its passwords, and the matching GitHub Actions secrets as critical recovery material.
