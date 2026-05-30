"""
Ereğli: Yeşil Dönüşüm — 16x16 blok texture üreticisi
Her blok için yan, üst ve alt yüzey PNG'leri oluşturur.
"""

from PIL import Image
import os
import random

OUT = "src/main/resources/assets/bsb-airquest/textures/block"
os.makedirs(OUT, exist_ok=True)

random.seed(42)  # Tekrarlanabilir sonuç


def save(img, name):
    path = os.path.join(OUT, name + ".png")
    img.save(path)
    print(f"  ✓ {name}.png")


def px(img, x, y, color):
    img.putpixel((x, y), color)


def fill(img, color):
    img.paste(color, [0, 0, 16, 16])


def noise(base, amount=15):
    """Renge hafif gürültü ekle (daha doğal görünüm)."""
    r, g, b = base
    d = random.randint(-amount, amount)
    return (max(0, min(255, r + d)),
            max(0, min(255, g + d)),
            max(0, min(255, b + d)))


# ─────────────────────────────────────────────────────────────────────────────
# KİRLİ BACA — tuğla + is/kurum efekti
# ─────────────────────────────────────────────────────────────────────────────
print("kirli_baca...")

# Yan yüzey: kirli tuğla deseni
img = Image.new("RGB", (16, 16))
BRICK  = (120, 60, 40)   # koyu kırmızı-kahve (tuğla)
MORTAR = (60, 60, 55)    # derz rengi (karanlık çimento)
SOOT   = (20, 18, 15)    # kurum/is lekesi

for y in range(16):
    for x in range(16):
        # Tuğla deseni: her iki satırda bir offset
        row = y // 4
        offset = 4 if row % 2 == 1 else 0
        brick_x = (x + offset) % 8

        if y % 4 == 3 or brick_x == 0:  # derz çizgileri
            color = noise(MORTAR, 8)
        else:
            color = noise(BRICK, 20)

        # Rastgele kurum lekesi
        if random.random() < 0.12:
            color = noise(SOOT, 5)

        img.putpixel((x, y), color)

save(img, "kirli_baca_side")

# Üst yüzey: duman deliği
img = Image.new("RGB", (16, 16))
DARK_HOLE = (15, 12, 10)
RIM       = (80, 45, 30)
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        dist = (dx**2 + dy**2) ** 0.5
        if dist < 4:
            img.putpixel((x, y), noise(DARK_HOLE, 5))
        elif dist < 6:
            img.putpixel((x, y), noise(SOOT, 10))
        else:
            img.putpixel((x, y), noise(RIM, 15))

save(img, "kirli_baca_top")
save(img, "kirli_baca_bottom")


# ─────────────────────────────────────────────────────────────────────────────
# TEMİZ BACA — beton + filtre ızgara efekti
# ─────────────────────────────────────────────────────────────────────────────
print("temiz_baca...")

# Yan yüzey: temiz beton
img = Image.new("RGB", (16, 16))
CONCRETE = (180, 175, 170)
SEAM     = (140, 135, 130)

for y in range(16):
    for x in range(16):
        if x == 0 or y == 0 or x == 15 or y == 15:
            color = noise(SEAM, 8)
        elif y % 5 == 4:
            color = noise(SEAM, 5)
        else:
            color = noise(CONCRETE, 12)
        img.putpixel((x, y), color)

save(img, "temiz_baca_side")

# Üst yüzey: filtre ızgarası
img = Image.new("RGB", (16, 16))
FILTER_BG   = (200, 200, 205)
FILTER_GRID = (100, 140, 180)
FILTER_HOLE = (50, 80, 120)

for y in range(16):
    for x in range(16):
        if x % 3 == 1 and y % 3 == 1:
            img.putpixel((x, y), noise(FILTER_HOLE, 10))
        elif x % 3 == 0 or y % 3 == 0:
            img.putpixel((x, y), noise(FILTER_GRID, 8))
        else:
            img.putpixel((x, y), noise(FILTER_BG, 12))

save(img, "temiz_baca_top")

# Alt yüzey: sade beton
img = Image.new("RGB", (16, 16))
for y in range(16):
    for x in range(16):
        img.putpixel((x, y), noise(CONCRETE, 10))
save(img, "temiz_baca_bottom")


# ─────────────────────────────────────────────────────────────────────────────
# DİZEL JENERATÖR — kirli endüstriyel makine
# ─────────────────────────────────────────────────────────────────────────────
print("dizel_jenerator...")

# Ön yüzey: kontrol paneli
img = Image.new("RGB", (16, 16))
METAL    = (70, 68, 65)
RUST     = (110, 50, 20)
GAUGE    = (200, 140, 40)   # sarı gösterge
LIGHT_ON = (220, 80, 30)    # kırmızı uyarı ışığı

for y in range(16):
    for x in range(16):
        if x == 0 or x == 15 or y == 0 or y == 15:
            # Çerçeve
            color = noise((50, 48, 45), 8)
        elif 4 <= x <= 6 and 4 <= y <= 9:
            # Hava deliği ızgarası
            if x == 5 or y % 2 == 0:
                color = noise((30, 28, 25), 5)
            else:
                color = noise(METAL, 10)
        elif 9 <= x <= 12 and 3 <= y <= 7:
            # Gösterge
            color = noise(GAUGE, 20)
        elif x == 11 and y == 11:
            # Kırmızı uyarı ışığı
            color = LIGHT_ON
        else:
            # Genel gövde + pas lekesi
            color = noise(METAL, 15)
            if random.random() < 0.08:
                color = noise(RUST, 20)
        img.putpixel((x, y), color)

save(img, "dizel_jenerator_front")

# Yan yüzey: metal gövde + boru çıkışı
img = Image.new("RGB", (16, 16))
PIPE = (55, 52, 48)

for y in range(16):
    for x in range(16):
        if x == 0 or x == 15 or y == 0 or y == 15:
            color = noise((50, 48, 45), 8)
        elif 6 <= x <= 9 and 2 <= y <= 8:
            # Boru / egzoz çıkışı
            color = noise(PIPE, 10)
        else:
            color = noise(METAL, 15)
            if random.random() < 0.06:
                color = noise(RUST, 25)
        img.putpixel((x, y), color)

save(img, "dizel_jenerator_side")

# Üst yüzey: kirli metal çatı
img = Image.new("RGB", (16, 16))
for y in range(16):
    for x in range(16):
        base = noise(METAL, 18)
        if (x + y) % 5 == 0:
            base = noise((50, 48, 45), 8)
        if random.random() < 0.07:
            base = noise(RUST, 20)
        img.putpixel((x, y), base)

save(img, "dizel_jenerator_top")

# Alt yüzey: metal zemin
img = Image.new("RGB", (16, 16))
for y in range(16):
    for x in range(16):
        img.putpixel((x, y), noise((55, 53, 50), 12))
save(img, "dizel_jenerator_bottom")


# ─────────────────────────────────────────────────────────────────────────────
# TEMİZ JENERATÖR — güneş paneli + modern gövde
# ─────────────────────────────────────────────────────────────────────────────
print("temiz_jenerator...")

# Panel yüzeyi: fotovoltaik hücre deseni
img = Image.new("RGB", (16, 16))
CELL_DARK = (20, 40, 100)    # koyu mavi hücre
CELL_MID  = (35, 60, 140)    # hücre
CELL_REFL = (80, 110, 200)   # yansıma
GRID      = (160, 180, 220)  # gümüş çerçeve

for y in range(16):
    for x in range(16):
        if x % 4 == 3 or y % 4 == 3:
            # Hücre çerçevesi
            color = noise(GRID, 8)
        else:
            # Hücre içi — köşeye yakın yansıma efekti
            cx, cy = x % 4, y % 4
            if cx == 0 and cy == 0:
                color = noise(CELL_REFL, 10)
            elif cx <= 1 and cy <= 1:
                color = noise(CELL_MID, 12)
            else:
                color = noise(CELL_DARK, 10)
        img.putpixel((x, y), color)

save(img, "temiz_jenerator_panel")

# Gövde yüzeyi: beyaz/gri modern kasa
img = Image.new("RGB", (16, 16))
WHITE_METAL = (220, 222, 225)
ACCENT      = (100, 160, 220)  # mavi aksanlı çizgi

for y in range(16):
    for x in range(16):
        if x == 0 or x == 15 or y == 0 or y == 15:
            color = noise((190, 192, 195), 8)
        elif y == 7 or y == 8:
            # Mavi şerit
            color = noise(ACCENT, 10)
        else:
            color = noise(WHITE_METAL, 10)
        img.putpixel((x, y), color)

save(img, "temiz_jenerator_side")

# Alt yüzey
img = Image.new("RGB", (16, 16))
for y in range(16):
    for x in range(16):
        img.putpixel((x, y), noise((180, 182, 185), 10))
save(img, "temiz_jenerator_bottom")

print("\nTüm texturalar oluşturuldu →", OUT)
