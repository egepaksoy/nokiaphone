# KISS — Keep It Seriously Simple

Akıllı telefonu yalnızca arama, SMS ve seçtiğin birkaç özellikle sınırlayan, sade bir Android ana ekranı.

- **Ana ekran:** saat, Telefon (arama + SMS) ve Uygulamalar. Saate dokun: alarmlar. Boş yere çift dokun: kilit.
- **Uygulamalar:** Kamera ve Galeri (telefonun kendi uygulamaları), Hesap makinesi, Notlar (seçtiğin uygulama), eklediğin uygulamalar
- **İzin listesi:** İstediğin uygulamaları ana ekrana ekle
- **Ayar kilidi:** Bekleme süresi + 4 haneli PIN
- **Sıkı mod:** Cihaz sahibi (Device Owner) + kiosk modu; izin listesi dışındaki uygulamalar açılamaz ve askıya alınır

Kurulum: [docs/KURULUM.md](docs/KURULUM.md) · Plan: [docs/PLAN.md](docs/PLAN.md) · Arayüz prototipi: [docs/design/prototype.html](docs/design/prototype.html)

## Derleme

```
export KISS_KEYSTORE=/yol/kiss.jks KISS_STORE_PASSWORD=...
./gradlew testDebugUnitTest assembleRelease
# çıktı: app/build/outputs/apk/release/app-release.apk
```

İmza anahtarı repoda tutulmaz. Anahtar yoksa APK debug anahtarıyla imzalanır; bu sadece deneme içindir, çünkü cihaz sahibi olan KISS'in güncellemeleri hep aynı anahtarla imzalanmalıdır.

GitHub Actions her push'ta APK derler (Actions > APK > `kiss-apk`). Sabit imza için repo sırlarına `KISS_KEYSTORE_BASE64` (`base64 -w0 kiss.jks`) ve `KISS_STORE_PASSWORD` ekle.

Testler (Robolectric) uygulamayı cihazsız çalıştırır ve ekran görüntülerini `app/build/shots/` altına kaydeder.

minSdk 29, targetSdk 36. Kotlin, Jetpack Compose.

Yazı tipleri: Familjen Grotesk ve Figtree (SIL Open Font License 1.1, Google Fonts).
