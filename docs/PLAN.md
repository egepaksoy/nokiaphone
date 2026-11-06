# Sade: Plan (v0.1)

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

## Açık kararlar
1. Başlangıç koruma seviyesi (öneri: Orta)
2. Telefon modeli ve Android sürümü
3. Uygulama adı ("Sade" çalışma adı)
4. Ek modüller: alarm, fener, hesap makinesi, notlar, müzik
