# KISS: Plan (v0.1)

> Uygulamanın adı "Sade" yerine **KISS** (Keep It Seriously Simple) oldu. Başlangıç seviyesi: **Sıkı**. Ek modüller: alarm, fener, hesap makinesi, notlar (notlar, kullanıcının seçtiği uygulamayı açar).

## Fikir
Sade, Android'de **varsayılan ana ekran (launcher)** olarak seçilir. Ana ekran tuşuna her basıldığında Sade açılır
ve yalnızca izin verilen şeyleri gösterir. Arama, mesaj, kamera ve galeri uygulamanın içinde yerleşik gelir.
Kullanıcı Ayarlar'dan modülleri (kamera, galeri, fener, alarm) açıp kapatabilir ve istediği uygulamaları izin listesine ekleyebilir.

## Koruma seviyeleri
| Seviye | Nasıl | Not |
|---|---|---|
| Hafif | Sadece launcher; gizli uygulamalar listede yok | Sistem ayarlarından kaçış mümkün |
| Orta (önerilen) | Launcher + erişilebilirlik bekçisi (izinsiz uygulama açılırsa ana ekrana dön) + bildirim filtresi | Android 13+ için "Kısıtlı ayarlara izin ver" adımı |
| Sıkı | Device Owner + Lock Task (kiosk) | `adb` ile bir kerelik kurulum, cihazda hesap olmamalı |

Arama ve Mesajlar kapatılamaz; acil arama her seviyede çalışır.
Ayarlara giriş: bekleme süresi (varsayılan 30 sn) + 4 haneli PIN. "Akıllı telefona dön" daha uzun bekleme ister.

## Teknik yapı
- Kotlin, Jetpack Compose + Material 3, tek Activity, Navigation Compose
- minSdk 26, targetSdk 36
- DataStore (ayarlar, izin listesi, PIN özeti)
- CameraX (kamera), MediaStore + Coil (galeri)
- Kişiler / arama kaydı / SMS sistem sağlayıcılarından okunur
- GitHub Actions ile her push'ta APK

Modüller: `launcher`, `phone`, `sms`, `camera`, `gallery`, `apps`, `settings`, `guard`, `admin`.

## İzinler
- `HOME` intent filtresi
- `CALL_PHONE`, `READ_CALL_LOG`, `READ_CONTACTS`
- `READ_SMS`, `SEND_SMS`, `RECEIVE_SMS` (ileride `ROLE_SMS`)
- `CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`
- `<queries>` ile launcher uygulamaları sorgusu
- Erişilebilirlik servisi, bildirim dinleyici (Orta/Sıkı)
- Device Owner (Sıkı)

## Yol haritası
0. **İskelet**: Gradle + Compose, tema, launcher kaydı, CI ile APK
1. **Çekirdek**: ana ekran, ayarlar, modül aç/kapa, uygulama izin listesi, ayar kilidi
2. **Arama**: tuş takımı, son aramalar, kişiler, favoriler
3. **SMS**: konuşma listesi, konuşma ekranı, gönderim, okunmamış rozeti
4. **Medya**: CameraX kamera, galeri
5. **Koruma**: erişilebilirlik bekçisi, bildirim filtresi, Device Owner + Lock Task, geçici izin (15 dk), sistem panelleri
6. **Cila**: varsayılan SMS/arama uygulaması olma, ekran süresi özeti, ek modüller

## Kararlar
1. Koruma seviyesi: Sıkı (Device Owner + Lock Task + gizli uygulamaları askıya alma)
2. Cihaz: Samsung Galaxy S25, Android 17
3. Ad: KISS
4. Ek modüller: alarm, fener, hesap makinesi, notlar

## v0.1 durumu
- [x] İskelet, tema, launcher kaydı, CI ile APK
- [x] Ana ekran, ayarlar, modüller, izin listesi, ayar kilidi (bekleme + PIN)
- [x] Arama: tuş takımı, son aramalar, kişiler
- [x] SMS: konuşmalar, konuşma, gönderim, gelen mesaj bildirimi
- [x] Kamera (CameraX, fotoğraf), galeri (görüntüleme, silme)
- [x] Hesap makinesi, fener, alarm, notlar
- [x] Sıkı mod: Device Owner, Lock Task, askıya alma, geçici sistem ayarı erişimi, acil çıkış
- [x] v0.2: Mesajlar'da SMS / WhatsApp seçimi (mesaj uygulamaları ayarlanabilir)
- [x] v0.2: Saate dokununca Saat uygulaması; sıradaki alarm tarihin yanında
- [x] v0.2: Boş alana çift dokunarak ekran kilidi
- [x] v0.3: Ana ekran sadece saat, Telefon ve Uygulamalar
- [x] v0.3: Arama ve SMS tek "Telefon" sayfasında (Tuşlar, Son, Mesajlar, Kişiler)
- [x] v0.3: Kamera ve galeri telefonun kendi uygulamalarını açar (yerleşik kamera/galeri kaldırıldı)
- [ ] Varsayılan SMS uygulaması olma (MMS dahil)
- [ ] Gerçek cihazda Samsung testleri
