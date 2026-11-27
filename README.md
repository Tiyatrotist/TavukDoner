# TavukDoner

🇹🇷 [Türkçe](#türkçe) · 🇬🇧 [English](#english)

Paper 26.3 için vanilla'yı ezmeyen, tek jar'lık, bağımlılıksız tavuk döner eklentisi. · A single-jar, dependency-free chicken doner kebab plugin for Paper 26.3 that does not override vanilla.

---

## Türkçe

### Nasıl oynanır
1. **Şiş kur:** Elinde blaze rod ile katı bir bloğun **üst yüzüne** sağ tıkla. Şiş (item display'lerden yapılmış, kaynak paketsiz) belirir.
2. **Et ekle:** Şişe çiğ tavukla sağ tıkla. Her tavuk bir katman ekler (en fazla 16), katmanlar üstte genişleyen hacimli bir döner konisi oluşturur.
3. **Pişir:** Şişin yanına veya altına (±1 blok) **yanan kamp ateşi** koy. Şiş döner ve katmanlar alttan üste doğru kademeli pişer.
4. **Dilimle:** Pişmiş şişe **herhangi bir kılıçla** sağ tıkla: "Döner Dilimi" düşer, bir katman azalır, kılıç aşınır. Katman bitince şiş kaybolur ve çubuk düşer.
5. **Ye:** Tezgâhta şekilsiz tarifler:
   | Tarif | Sonuç |
   |---|---|
   | Döner Dilimi + Ekmek | Tavuk Döner Ekmek (kalite 1) |
   | + Patates **veya** pancar | kalite 2 |
   | + Patates **ve** pancar | kalite 3 |

   Vanilla'da domates olmadığı için pancar domates yerine geçer (`topping-material` ile değişir). Yenince doygunluk ve kısa süreli buff verir; kalite başına ek süre eklenir.

Sol tıkla şişi sökersen çubuk ve et geri düşer. Bedrock (Geyser/Floodgate) oyuncularda da çalışır: özel model yok, yalnızca vanilla eşyalar.

### Kurulum
`TavukDoner-<sürüm>.jar` dosyasını `plugins/` klasörüne koy, sunucuyu başlat. Paper 26.3 ve Java 25 gerekir.

### Komutlar ve izinler
| | |
|---|---|
| `/tavukdoner` (`/doner`) | Sürüm bilgisi |
| `/tavukdoner reload` | Config, mesajlar ve tarifleri yeniler (`tavukdoner.admin`) |
| `tavukdoner.use` | Şişi kullanma ve tarifi yapma (varsayılan: herkes) |
| `tavukdoner.admin` | Yönetim komutları, claim kontrolünü atlama (varsayılan: op) |

### GriefPrevention
Yüklüyse (soft dependency, reflection ile), claim içinde yalnızca sahip ve güvenilir (trust) oyuncular şişe müdahale edebilir. Yüklü değilse koruma olmadan çalışır.

### Ayarlar
`plugins/TavukDoner/config.yml` (pişme süresi, katman sayısı, chunk ve oyuncu limitleri, animasyon, tarif, buff) ve `messages.yml` (TR + EN, `language: tr|en`). Her anahtar dosyada açıklamalı.

### Performans
Şişler chunk PersistentDataContainer'da saklanır. Entity'ler kalıcı değildir; chunk yüklenince kurulur, boşalınca silinir. Chunk yüklü değilken hiçbir iş yapılmaz. Şiş sayısı chunk ve oyuncu başına sınırlıdır; dönme animasyonu yalnızca yakında oyuncu varsa çalışır.

### Derleme
```
./gradlew build
```
Çıktı: `build/libs/TavukDoner-<sürüm>.jar` (JDK 25 toolchain otomatik indirilir).

### Lisans
MIT, bkz. [LICENSE](LICENSE).

---

## English

### How to play
1. **Build a skewer:** Right-click the **top face** of a solid block with a blaze rod. A skewer (made of item displays, no resource pack) appears.
2. **Add meat:** Right-click the skewer with raw chicken. Each chicken adds a layer (max 16), forming a volumetric doner cone, wider at the top.
3. **Cook:** Put a **lit campfire** next to or under the skewer (±1 block). It spins and layers cook gradually from the bottom up.
4. **Carve:** Right-click a cooked skewer with **any sword** to drop "Doner Slice" items. It removes one layer and damages the sword. When empty, the skewer disappears and the rod drops.
5. **Eat:** Shapeless crafting recipes:
   | Recipe | Result |
   |---|---|
   | Doner Slice + Bread | Chicken Doner Sandwich (tier 1) |
   | + Potato **or** beetroot | tier 2 |
   | + Potato **and** beetroot | tier 3 |

   Vanilla has no tomato, so beetroot stands in for it (`topping-material`). Eating gives hunger, saturation and a short buff; higher tiers last longer.

Left-click a skewer to take it down and get the rod and meat back. Works for Bedrock (Geyser/Floodgate) players: no custom models, vanilla items only.

### Install
Drop `TavukDoner-<version>.jar` into `plugins/` and start the server. Requires Paper 26.3 and Java 25.

### Commands and permissions
| | |
|---|---|
| `/tavukdoner` (`/doner`) | Version info |
| `/tavukdoner reload` | Reload config, messages and recipes (`tavukdoner.admin`) |
| `tavukdoner.use` | Use skewers and craft (default: everyone) |
| `tavukdoner.admin` | Admin commands, bypass claim checks (default: op) |

### GriefPrevention
If installed (soft dependency, via reflection), only the claim owner and trusted players can interact with skewers inside a claim. Without it the plugin runs unprotected.

### Configuration
`plugins/TavukDoner/config.yml` (cook time, layers, chunk and player limits, animation, recipes, buffs) and `messages.yml` (TR + EN, `language: tr|en`).

### Performance
Skewers live in the chunk PersistentDataContainer. Display entities are non-persistent: spawned on chunk load, removed on unload, nothing ticks for unloaded chunks. Skewer counts are capped per chunk and per player; the spin animation only runs when a player is nearby.

### Build
```
./gradlew build
```
Output: `build/libs/TavukDoner-<version>.jar` (JDK 25 toolchain is downloaded automatically).

### License
MIT, see [LICENSE](LICENSE).
