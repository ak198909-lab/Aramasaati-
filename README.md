# Arama Saati (Android)

Seçtiğin kişilerin seni hangi saatlerde arayabileceğini belirler. Mesajlar (SMS) hiç etkilenmez.

## Örnek kurallar
- **Sadece bu saatlerde arayabilsin:** Ahmet → Hafta içi 08:00–18:00. Bunun dışında ve hafta sonu arayamaz.
- **Bu saatlerde arayamasın:** Patron → Her gün 12:00–13:00 (öğle arası) arayamaz, diğer saatlerde arayabilir.
- Bir kurala birden fazla saat aralığı ekleyebilirsin (ör. 09:00–12:00 ve 13:00–18:00).
- Gece yarısını geçen aralık da olur (ör. 22:00–07:00).

## Kurulum
1. Android Studio'yu aç → **File › Open** → bu `AramaSaati` klasörünü seç.
2. Gradle eşitlemesi bitince telefonu USB ile bağla (Geliştirici seçenekleri › USB hata ayıklama açık olmalı).
3. ▶ **Run** düğmesine bas. APK istersen: **Build › Build APK(s)**.

## İlk açılışta
1. Rehber ve bildirim iznini ver.
2. Uygulamayı **"Arayan kimliği ve spam uygulaması"** (arama filtreleme) olarak seç.
   Bu izin olmadan telefon aramaları uygulamaya sormaz.
3. "+ Kişi ekle" → rehberden kişiyi seç → kural türü, günler ve saatleri ayarla → Kaydet.

## Bilmen gerekenler
- Android 10 ve üzeri gerekir.
- Engellenen arama reddedilir, telefon çalmaz; arayan kişi meşgul/ulaşılamıyor duyar.
  Arama, telefonun arama geçmişinde görünür; ayrıca uygulamada "Engellenen aramalar" listesine düşer.
- WhatsApp, Telegram gibi internet üzerinden gelen aramalar bu yöntemle engellenemez (Android izin vermiyor).
- Gizli numarayla gelen aramada numara bilinmediği için kural uygulanamaz.
- Bazı Xiaomi/Huawei telefonlarda pil tasarrufu uygulamayı kısıtlayabilir; Ayarlar'dan "kısıtlama yok" seçmek iyi olur.
