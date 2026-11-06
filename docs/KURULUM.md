# KISS kurulum rehberi (Samsung Galaxy S25)

KISS iki şekilde çalışır:

- **Normal:** KISS'i kur ve varsayılan ana ekran yap. Başka bir şey gerekmez, ama Android ayarlarından kolayca çıkılabilir.
- **Sıkı mod:** KISS cihaz sahibi (Device Owner) olur ve telefonu kiosk moduna alır. İzin listesi dışında hiçbir uygulama açılamaz. Bunun için bir kez bilgisayar gerekir.

## Gerekenler

- Bilgisayar (Windows, macOS ya da Linux) ve USB kablosu
- [Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools) (içinden `adb` kullanılacak)
- `kiss.apk` dosyası

## 1. Telefonu hazırla

1. **Ayarlar > Telefon hakkında > Yazılım bilgileri**'nde **Yapım numarası**'na 7 kez dokun. Geliştirici seçenekleri açılır.
2. **Ayarlar > Geliştirici seçenekleri > USB hata ayıklama**'yı aç.
3. **Ayarlar > Güvenlik ve gizlilik > Otomatik Engelleyici**'yi kapat. Açıkken USB komutları ve APK kurulumu engellenir.

## 2. KISS'i kur

```
adb devices                 # telefonda "USB hata ayıklamaya izin ver" çıkarsa onayla
adb install kiss.apk
```

KISS'i aç, PIN belirle, izinleri ver ve ana ekran yap. Normal mod için kurulum burada biter.

## 3. Sıkı mod: cihaz sahibi yap

Android, cihaz sahibi atamayı yalnızca telefonda **hiç hesap yokken** ve **tek kullanıcı varken** kabul eder.

1. **Ayarlar > Hesaplar ve yedekleme > Hesapları yönet**'ten tüm hesapları kaldır: Samsung hesabı, Google hesabı, WhatsApp, Telegram, Microsoft vb. Fotoğraflar, rehber ve mesajlar telefonda kalır; hesaplar sonra geri eklenir. Bulut yedeğin güncel olsun.
   Kalan hesapları görmek için: `adb shell dumpsys account | grep "Account {"`
2. **Güvenli Klasör** varsa kaldır. **Gelişmiş özellikler > Dual Messenger** açıksa kapat. Kontrol: `adb shell pm list users` yalnızca `UserInfo{0:...}` göstermeli.
3. Komutu çalıştır:

   ```
   adb shell dpm set-device-owner com.egepaksoy.kiss/.admin.KissAdminReceiver
   ```

   `Success: Device owner set to package ...` görmelisin.
4. Hesaplarını geri ekle.
5. KISS > Ayarlar > **Sıkı mod**'u aç.

> Hızlı ayarlar panelinde "Bu cihaz kuruluşunuz tarafından yönetiliyor" yazısı görünür. Bu yönetici KISS'in kendisidir; telefon başka bir yere bağlı değildir. KISS internete hiç bağlanmaz.

## Önerilen ayarlar

- **Samsung Mesajlar bildirimlerini kapat** (Ayarlar > Bildirimler > Mesajlar). Gelen SMS bildirimini KISS kendisi gösterir; ikisi birden açıksa her mesaj için iki bildirim gelir.
- Sıkı modda bildirim paneli varsayılan olarak açıktır (Wi-Fi, parlaklık, rahatsız etme için). Kapatmak istersen KISS > Ayarlar > Sıkı mod > Bildirim paneli.

## Sıkı modda ne çalışır?

| Çalışır | Çalışmaz |
|---|---|
| Gelen/giden aramalar, acil arama | Son uygulamalar ekranı |
| SMS (KISS içinde) | İzin listesinde olmayan uygulamalar |
| Alarm çalması (Saat uygulaması) | Gizli uygulamaların bildirimleri (askıya alınırlar) |
| İzin verdiğin uygulamalar ve not uygulaman | Sistem ayarları (KISS > Ayarlar > Sistem'den geçici olarak açılır) |
| Güç menüsü, kilit ekranı | |

## Güncelleme

Yeni bir `kiss.apk` geldiğinde:

```
adb install -r kiss.apk
```

Sürümler aynı anahtarla imzalandığı sürece ayarlar ve cihaz sahipliği korunur. **İmza anahtarını (kiss.jks ve parolası) kaybetme:** farklı anahtarla imzalanmış bir sürüm, cihaz sahibi olan KISS'in üzerine kurulamaz.

## Çıkış

- **Normal çıkış:** KISS > Ayarlar > Sıkı mod > kapat (2 dakika bekleme).
- **KISS'i tamamen kaldırmak:** KISS > Ayarlar > Sıkı mod > Cihaz sahipliğini kaldır. Sonra uygulama normal şekilde silinir.
- **Acil çıkış** (KISS açılmıyorsa, bilgisayardan):

  ```
  adb shell am broadcast -a com.egepaksoy.kiss.EMERGENCY_EXIT -n com.egepaksoy.kiss/.admin.EmergencyExitReceiver
  ```

  Cihaz sahipliğini de kaldırmak için komutun sonuna `--ez remove_owner true` ekle.
