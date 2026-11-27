# Changelog

## 0.2.1 (2026-11-27)

- Şişe artık 20 katman et dizilir, çubuğun tamamı dolar (`max-layers`, üst sınır 20).
- Pişme çok daha yavaş: dolu şiş 5 dakikada pişer (`cook-time-seconds: 300`, öncesi 120).
- Skewers now hold 20 layers (fills the whole rod); cooking is slower (300 s instead of 120 s). Existing servers keep their old `config.yml` values; edit `max-layers` and `cook-time-seconds` or delete the file to regenerate.

## 0.2.0 (2026-11-27)

- Yeni görünüm: `BlockDisplay` ile hacimli, üstte genişleyen döner konisi, tepsi ve metal şiş çubuğu; bantlı çiğ/pişmiş renkler (`look.raw-blocks`, `look.cooked-blocks`).
- Şiş kapasitesi 8 → 16 katman (`max-layers`, üst sınır 24).
- New look: volumetric block-display doner cone with tray and rod; max layers 8 → 16.

## 0.1.0 (2026-11-22)

İlk sürüm / Initial release.

- Şiş kurma, et ekleme, sökme; `item_display` koni görünümü, chunk PDC kalıcılığı.
- Yanan kamp ateşiyle kademeli pişme ve dönme animasyonu.
- Kılıçla dilimleme, "Döner Dilimi" eşyası, kılıç aşınması.
- "Tavuk Döner Ekmek" tarifleri (3 kalite) ve buff.
- GriefPrevention soft dependency, chunk ve oyuncu başı şiş limiti.
- TR + EN mesajlar, `tavukdoner.use` / `tavukdoner.admin` izinleri.
