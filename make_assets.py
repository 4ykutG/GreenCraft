"""
Ereğli: Yeşil Dönüşüm — Kapsamlı Asset Üreticisi
22 blok için texture (PNG), blockstate, model ve item model JSON'larını üretir.
"""

from PIL import Image
import json, os, random

random.seed(42)

BASE = "src/main/resources/assets/bsb-airquest"
TEX  = f"{BASE}/textures/block"
BS   = f"{BASE}/blockstates"
MB   = f"{BASE}/models/block"
MI   = f"{BASE}/models/item"
LANG = f"{BASE}/lang"

for d in [TEX, BS, MB, MI, LANG]:
    os.makedirs(d, exist_ok=True)

MOD = "bsb-airquest"

# ─── Texture yardımcıları ──────────────────────────────────────────────────────

def img():
    return Image.new("RGB", (16, 16))

def noise(c, a=15):
    return tuple(max(0, min(255, v + random.randint(-a, a))) for v in c)

def save(im, name):
    im.save(f"{TEX}/{name}.png")
    print(f"  tex {name}.png")

def fill(color, variation=12):
    i = img()
    for y in range(16):
        for x in range(16):
            i.putpixel((x, y), noise(color, variation))
    return i

def cracked(base, crack, variation=18):
    i = img()
    for y in range(16):
        for x in range(16):
            is_crack = ((x * 3 + y * 2) % 9 == 0) or (random.random() < 0.04)
            i.putpixel((x, y), noise(crack if is_crack else base, variation))
    return i

def metal(color, rust=0.06, grid=False):
    RUST = (110, 55, 20)
    i = img()
    for y in range(16):
        for x in range(16):
            if grid and (x % 4 == 0 or y % 4 == 0):
                c = noise(tuple(max(0, v - 30) for v in color), 8)
            elif random.random() < rust:
                c = noise(RUST, 20)
            else:
                c = noise(color, 14)
            i.putpixel((x, y), c)
    return i

def with_border(base_fn, border_color, border=1):
    i = base_fn()
    for y in range(16):
        for x in range(16):
            if x < border or x >= 16-border or y < border or y >= 16-border:
                i.putpixel((x, y), noise(border_color, 8))
    return i

def solar_panel():
    CELL  = (20, 40, 100)
    MID   = (35, 60, 140)
    REFL  = (80, 110, 200)
    GRID  = (160, 180, 220)
    i = img()
    for y in range(16):
        for x in range(16):
            if x % 4 == 3 or y % 4 == 3:
                i.putpixel((x, y), noise(GRID, 8))
            else:
                cx, cy = x % 4, y % 4
                if cx == 0 and cy == 0:
                    i.putpixel((x, y), noise(REFL, 10))
                elif cx <= 1 and cy <= 1:
                    i.putpixel((x, y), noise(MID, 12))
                else:
                    i.putpixel((x, y), noise(CELL, 10))
    return i

def filter_grid():
    BG  = (200, 200, 205)
    GRD = (100, 140, 180)
    HOL = (50, 80, 120)
    i = img()
    for y in range(16):
        for x in range(16):
            if x % 3 == 1 and y % 3 == 1:
                i.putpixel((x, y), noise(HOL, 10))
            elif x % 3 == 0 or y % 3 == 0:
                i.putpixel((x, y), noise(GRD, 8))
            else:
                i.putpixel((x, y), noise(BG, 12))
    return i

def recycle_symbol(base_color, arrow_color):
    i = fill(base_color, 10)
    # Basit ok deseni: köşegen çizgiler
    for y in range(16):
        for x in range(16):
            if abs(x - y) <= 1 or abs(x - (15 - y)) <= 1:
                if 3 <= x <= 12 and 3 <= y <= 12:
                    i.putpixel((x, y), noise(arrow_color, 8))
    return i

# ─── JSON yardımcıları ─────────────────────────────────────────────────────────

def write_json(path, data):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

def blockstate(block_id):
    write_json(f"{BS}/{block_id}.json",
        {"variants": {"": {"model": f"{MOD}:block/{block_id}"}}})

def item_model(block_id):
    write_json(f"{MI}/{block_id}.json", {"parent": f"{MOD}:block/{block_id}"})

def model_cube_all(block_id, tex=None):
    write_json(f"{MB}/{block_id}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": tex or f"{MOD}:block/{block_id}"}
    })

def model_cube(block_id, top, bottom, north, side):
    write_json(f"{MB}/{block_id}.json", {
        "parent": "minecraft:block/cube",
        "textures": {
            "up": top, "down": bottom,
            "north": north, "south": side, "east": side, "west": side,
            "particle": side
        }
    })

def model_chimney(block_id, side, top, bottom):
    write_json(f"{MB}/{block_id}.json", {
        "parent": "minecraft:block/block",
        "textures": {"side": side, "top": top, "bottom": bottom, "particle": side},
        "elements": [{
            "from": [5, 0, 5], "to": [11, 16, 11],
            "faces": {
                "north": {"uv": [0,0,16,16], "texture": "#side"},
                "south": {"uv": [0,0,16,16], "texture": "#side"},
                "east":  {"uv": [0,0,16,16], "texture": "#side"},
                "west":  {"uv": [0,0,16,16], "texture": "#side"},
                "up":    {"uv": [0,0,16,16], "texture": "#top"},
                "down":  {"uv": [0,0,16,16], "texture": "#bottom", "cullface": "down"}
            }
        }]
    })

def model_panel_base(block_id, panel, side, bottom):
    """Altta gövde + üstte güneş paneli levhası."""
    write_json(f"{MB}/{block_id}.json", {
        "parent": "minecraft:block/block",
        "textures": {"panel": panel, "side": side, "bottom": bottom, "particle": panel},
        "elements": [
            {
                "from": [2, 0, 2], "to": [14, 10, 14],
                "faces": {
                    "north": {"uv": [0,0,16,16], "texture": "#side"},
                    "south": {"uv": [0,0,16,16], "texture": "#side"},
                    "east":  {"uv": [0,0,16,16], "texture": "#side"},
                    "west":  {"uv": [0,0,16,16], "texture": "#side"},
                    "down":  {"uv": [0,0,16,16], "texture": "#bottom", "cullface": "down"}
                }
            },
            {
                "from": [0, 10, 0], "to": [16, 13, 16],
                "faces": {
                    "north": {"uv": [0,0,16,3], "texture": "#side"},
                    "south": {"uv": [0,0,16,3], "texture": "#side"},
                    "east":  {"uv": [0,0,16,3], "texture": "#side"},
                    "west":  {"uv": [0,0,16,3], "texture": "#side"},
                    "up":    {"uv": [0,0,16,16], "texture": "#panel"}
                }
            }
        ]
    })

def model_flat_roof_panel(block_id, panel, side):
    """Çatı güneş paneli: tam genişlik ama biraz kısa."""
    write_json(f"{MB}/{block_id}.json", {
        "parent": "minecraft:block/block",
        "textures": {"panel": panel, "side": side, "particle": panel},
        "elements": [{
            "from": [0, 0, 0], "to": [16, 12, 16],
            "faces": {
                "north": {"uv": [0,4,16,16], "texture": "#side", "cullface": "north"},
                "south": {"uv": [0,4,16,16], "texture": "#side", "cullface": "south"},
                "east":  {"uv": [0,4,16,16], "texture": "#side", "cullface": "east"},
                "west":  {"uv": [0,4,16,16], "texture": "#side", "cullface": "west"},
                "up":    {"uv": [0,0,16,16], "texture": "#panel"},
                "down":  {"uv": [0,0,16,16], "texture": "#side", "cullface": "down"}
            }
        }]
    })

def scaffold(block_id):
    blockstate(block_id)
    item_model(block_id)

# ══════════════════════════════════════════════════════════════════════════════
#  BLOK ASSET'LERİ
# ══════════════════════════════════════════════════════════════════════════════

print("=== AŞAMA 1: FABRIKA ===")

# ── kirli_baca ────────────────────────────────────────────────────────────────
BRICK  = (120, 60, 40)
MORTAR = (60, 60, 55)
SOOT   = (20, 18, 15)

i = img()
for y in range(16):
    for x in range(16):
        row = y // 4
        bx  = (x + (4 if row % 2 else 0)) % 8
        col = MORTAR if (y % 4 == 3 or bx == 0) else BRICK
        if random.random() < 0.12:
            col = SOOT
        i.putpixel((x, y), noise(col, 20))
save(i, "kirli_baca_side")

i = img()
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        d = (dx**2 + dy**2)**0.5
        c = SOOT if d < 4 else (MORTAR if d < 6 else BRICK)
        i.putpixel((x, y), noise(c, 12))
save(i, "kirli_baca_top")
save(fill(MORTAR, 10), "kirli_baca_bottom")

scaffold("kirli_baca")
model_chimney("kirli_baca",
    f"{MOD}:block/kirli_baca_side",
    f"{MOD}:block/kirli_baca_top",
    f"{MOD}:block/kirli_baca_bottom")

# ── temiz_baca ────────────────────────────────────────────────────────────────
CONCRETE = (180, 175, 170)
save(fill(CONCRETE, 12), "temiz_baca_side")
save(filter_grid(),      "temiz_baca_top")
save(fill((165,160,155), 10), "temiz_baca_bottom")

scaffold("temiz_baca")
model_chimney("temiz_baca",
    f"{MOD}:block/temiz_baca_side",
    f"{MOD}:block/temiz_baca_top",
    f"{MOD}:block/temiz_baca_bottom")

# ── dizel_jenerator ───────────────────────────────────────────────────────────
METAL_DRK = (70, 68, 65)
RUST      = (110, 50, 20)
GAUGE     = (200, 140, 40)
WARN_LED  = (220, 80, 30)

i = img()
for y in range(16):
    for x in range(16):
        if x == 0 or x == 15 or y == 0 or y == 15:
            c = noise((50, 48, 45), 8)
        elif 4 <= x <= 6 and 4 <= y <= 9:
            c = noise((30, 28, 25), 5) if (x == 5 or y % 2 == 0) else noise(METAL_DRK, 10)
        elif 9 <= x <= 12 and 3 <= y <= 7:
            c = noise(GAUGE, 20)
        elif x == 11 and y == 11:
            c = WARN_LED
        else:
            c = noise(RUST, 20) if random.random() < 0.08 else noise(METAL_DRK, 15)
        i.putpixel((x, y), c)
save(i, "dizel_jenerator_front")

i = img()
for y in range(16):
    for x in range(16):
        if x == 0 or x == 15 or y == 0 or y == 15:
            c = noise((50, 48, 45), 8)
        elif 6 <= x <= 9 and 2 <= y <= 8:
            c = noise((55, 52, 48), 10)
        else:
            c = noise(RUST, 25) if random.random() < 0.06 else noise(METAL_DRK, 15)
        i.putpixel((x, y), c)
save(i, "dizel_jenerator_side")
save(metal(METAL_DRK, rust=0.07), "dizel_jenerator_top")
save(fill((55, 53, 50), 12),       "dizel_jenerator_bottom")

scaffold("dizel_jenerator")
model_cube("dizel_jenerator",
    f"{MOD}:block/dizel_jenerator_top",
    f"{MOD}:block/dizel_jenerator_bottom",
    f"{MOD}:block/dizel_jenerator_front",
    f"{MOD}:block/dizel_jenerator_side")

# ── temiz_jenerator ───────────────────────────────────────────────────────────
WHITE_METAL = (220, 222, 225)
ACCENT_BLUE = (100, 160, 220)

i = img()
for y in range(16):
    for x in range(16):
        if x == 0 or x == 15 or y == 0 or y == 15:
            c = noise((190, 192, 195), 8)
        elif y in (7, 8):
            c = noise(ACCENT_BLUE, 10)
        else:
            c = noise(WHITE_METAL, 10)
        i.putpixel((x, y), c)
save(i,            "temiz_jenerator_side")
save(solar_panel(),"temiz_jenerator_panel")
save(fill((180, 182, 185), 10), "temiz_jenerator_bottom")

scaffold("temiz_jenerator")
model_panel_base("temiz_jenerator",
    f"{MOD}:block/temiz_jenerator_panel",
    f"{MOD}:block/temiz_jenerator_side",
    f"{MOD}:block/temiz_jenerator_bottom")

# ── kirli_su_tanki ────────────────────────────────────────────────────────────
DIRTY_BLUE = (40, 55, 90)

i = metal(DIRTY_BLUE, rust=0.10)
for y in range(16):   # yatay halkalar
    if y % 5 == 4:
        for x in range(16):
            i.putpixel((x, y), noise((25, 38, 65), 8))
save(i, "kirli_su_tanki_side")

i = img()
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        c = (15, 25, 40) if (dx**2+dy**2)**0.5 < 6 else noise(DIRTY_BLUE, 15)
        i.putpixel((x, y), c)
save(i, "kirli_su_tanki_top")

scaffold("kirli_su_tanki")
model_cube("kirli_su_tanki",
    f"{MOD}:block/kirli_su_tanki_top",
    f"{MOD}:block/kirli_su_tanki_side",
    f"{MOD}:block/kirli_su_tanki_side",
    f"{MOD}:block/kirli_su_tanki_side")

# ── aritma_tanki ──────────────────────────────────────────────────────────────
CLEAN_BLUE = (60, 130, 200)

i = img()
for y in range(16):
    for x in range(16):
        if y % 5 == 4:
            c = noise((40, 100, 160), 8)
        elif x == 0 or x == 15:
            c = noise((200, 200, 205), 8)
        else:
            c = noise(CLEAN_BLUE, 12)
        i.putpixel((x, y), c)
save(i, "aritma_tanki_side")

i = img()
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        c = (20, 180, 230) if (dx**2+dy**2)**0.5 < 5 else noise(CLEAN_BLUE, 10)
        i.putpixel((x, y), c)
save(i, "aritma_tanki_top")

scaffold("aritma_tanki")
model_cube("aritma_tanki",
    f"{MOD}:block/aritma_tanki_top",
    f"{MOD}:block/aritma_tanki_side",
    f"{MOD}:block/aritma_tanki_side",
    f"{MOD}:block/aritma_tanki_side")

# ── sogutma_bloku ─────────────────────────────────────────────────────────────
COOL_GRAY = (90, 95, 100)

i = img()
for y in range(16):
    for x in range(16):
        if x % 3 == 1:
            c = noise((60, 65, 70), 8)
        elif y % 4 == 0 or y % 4 == 3:
            c = noise((110, 115, 120), 8)
        else:
            c = noise(COOL_GRAY, 12)
        i.putpixel((x, y), c)
save(i, "sogutma_bloku_side")

i = img()
for y in range(16):
    for x in range(16):
        if abs(x - 7) + abs(y - 7) < 4:
            c = noise((140, 200, 220), 10)
        elif (x % 4 == 1 and 2 <= y <= 13) or (y % 4 == 1 and 2 <= x <= 13):
            c = noise((70, 75, 80), 8)
        else:
            c = noise(COOL_GRAY, 12)
        i.putpixel((x, y), c)
save(i, "sogutma_bloku_top")

scaffold("sogutma_bloku")
model_cube("sogutma_bloku",
    f"{MOD}:block/sogutma_bloku_top",
    f"{MOD}:block/sogutma_bloku_side",
    f"{MOD}:block/sogutma_bloku_side",
    f"{MOD}:block/sogutma_bloku_side")

# ── isi_esanjoru ──────────────────────────────────────────────────────────────
COPPER_COL = (185, 100, 45)

i = img()
for y in range(16):
    for x in range(16):
        if x % 5 == 2:
            c = noise((140, 75, 30), 8)
        elif y % 3 == 0:
            c = noise((200, 115, 55), 8)
        else:
            c = noise(COPPER_COL, 15)
        if random.random() < 0.04:
            c = noise((80, 45, 15), 10)
        i.putpixel((x, y), c)
save(i, "isi_esanjoru")

scaffold("isi_esanjoru")
model_cube_all("isi_esanjoru")

print("=== AŞAMA 2: KENT ===")

# ── corak_toprak ──────────────────────────────────────────────────────────────
save(cracked((130, 100, 65), (80, 60, 35)), "corak_toprak")
scaffold("corak_toprak")
model_cube_all("corak_toprak")

# ── yesil_alan ────────────────────────────────────────────────────────────────
GREEN_TOP = (80, 150, 50)
DIRT_SIDE = (110, 75, 40)
FLOWER    = [(230, 80, 80), (255, 220, 50), (255, 255, 255)]

i = fill(GREEN_TOP, 20)
for _ in range(6):
    fx, fy = random.randint(1, 14), random.randint(1, 14)
    fc = random.choice(FLOWER)
    i.putpixel((fx, fy), fc)
save(i, "yesil_alan_top")
save(cracked(DIRT_SIDE, (80, 50, 25)), "yesil_alan_side")

scaffold("yesil_alan")
model_cube("yesil_alan",
    f"{MOD}:block/yesil_alan_top",
    f"{MOD}:block/yesil_alan_side",
    f"{MOD}:block/yesil_alan_side",
    f"{MOD}:block/yesil_alan_side")

# ── bozuk_asfalt ──────────────────────────────────────────────────────────────
save(cracked((55, 55, 55), (25, 25, 25), 14), "bozuk_asfalt")
scaffold("bozuk_asfalt")
model_cube_all("bozuk_asfalt")

# ── bisiklet_seridi ───────────────────────────────────────────────────────────
ASPHALT  = (50, 50, 50)
BIKE_GRN = (50, 180, 80)

i = fill(ASPHALT, 10)
for x in range(4, 12):   # yeşil şerit
    for y in range(6, 10):
        if y == 6 or y == 9:
            i.putpixel((x, y), noise(BIKE_GRN, 8))
        elif x in (4, 11):
            i.putpixel((x, y), noise(BIKE_GRN, 8))
save(i, "bisiklet_seridi_top")
save(fill(ASPHALT, 8), "bisiklet_seridi_side")

scaffold("bisiklet_seridi")
model_cube("bisiklet_seridi",
    f"{MOD}:block/bisiklet_seridi_top",
    f"{MOD}:block/bisiklet_seridi_side",
    f"{MOD}:block/bisiklet_seridi_side",
    f"{MOD}:block/bisiklet_seridi_side")

# ── beton_cati ────────────────────────────────────────────────────────────────
save(fill((160, 158, 155), 12), "beton_cati")
scaffold("beton_cati")
model_cube_all("beton_cati")

# ── yesil_cati ────────────────────────────────────────────────────────────────
GRN_TOP  = (60, 140, 45)
GRAY_SID = (150, 148, 145)

i = fill(GRN_TOP, 20)
for _ in range(8):
    i.putpixel((random.randint(1,14), random.randint(1,14)), random.choice(FLOWER))
save(i, "yesil_cati_top")
save(fill(GRAY_SID, 10), "yesil_cati_side")

scaffold("yesil_cati")
model_cube("yesil_cati",
    f"{MOD}:block/yesil_cati_top",
    f"{MOD}:block/yesil_cati_side",
    f"{MOD}:block/yesil_cati_side",
    f"{MOD}:block/yesil_cati_side")

# ── benzin_istasyonu ──────────────────────────────────────────────────────────
PUMP_RED = (180, 40, 30)
PUMP_GRY = (100, 98, 95)

i = img()
for y in range(16):
    for x in range(16):
        if x == 0 or x == 15 or y == 0 or y == 15:
            c = noise(PUMP_GRY, 8)
        elif 5 <= x <= 10 and 3 <= y <= 12:
            c = noise(PUMP_RED, 15)
        elif 6 <= x <= 9 and 13 <= y <= 14:
            c = noise((50, 50, 50), 5)
        else:
            c = noise(PUMP_GRY, 12)
        i.putpixel((x, y), c)
save(i, "benzin_istasyonu_front")

i = fill(PUMP_GRY, 12)
for y in range(16):
    for x in range(16):
        if 6 <= x <= 9 and 2 <= y <= 8:
            i.putpixel((x, y), noise((40, 40, 40), 8))
save(i, "benzin_istasyonu_side")
save(fill((80, 78, 75), 10), "benzin_istasyonu_top")

scaffold("benzin_istasyonu")
model_cube("benzin_istasyonu",
    f"{MOD}:block/benzin_istasyonu_top",
    f"{MOD}:block/benzin_istasyonu_top",
    f"{MOD}:block/benzin_istasyonu_front",
    f"{MOD}:block/benzin_istasyonu_side")

# ── ev_sarj_istasyonu ─────────────────────────────────────────────────────────
EV_WHITE = (235, 237, 240)
EV_BLUE  = (30, 120, 200)
BOLT_YLW = (255, 210, 0)

i = fill(EV_WHITE, 8)
for y in range(16):
    for x in range(16):
        # Şarj şimşeği (yıldırım sembolü)
        if (4 <= x <= 11 and 3 <= y <= 7 and x + y < 16) or \
           (4 <= x <= 11 and 8 <= y <= 12 and x + y > 15):
            if 5 <= x <= 10:
                i.putpixel((x, y), noise(BOLT_YLW, 12))
        if y == 2 or y == 13 or x == 2 or x == 13:
            i.putpixel((x, y), noise(EV_BLUE, 10))
save(i, "ev_sarj_istasyonu_front")
save(fill(EV_WHITE, 8), "ev_sarj_istasyonu_side")
save(fill((210, 212, 215), 8), "ev_sarj_istasyonu_top")

scaffold("ev_sarj_istasyonu")
model_cube("ev_sarj_istasyonu",
    f"{MOD}:block/ev_sarj_istasyonu_top",
    f"{MOD}:block/ev_sarj_istasyonu_top",
    f"{MOD}:block/ev_sarj_istasyonu_front",
    f"{MOD}:block/ev_sarj_istasyonu_side")

print("=== AŞAMA 3: TOPLULUK ===")

# ── atik_alani ────────────────────────────────────────────────────────────────
DEBRIS = [(80, 78, 75), (60, 55, 50), (100, 90, 80)]

i = fill((65, 63, 60), 12)
for _ in range(12):
    x, y = random.randint(0, 15), random.randint(0, 15)
    i.putpixel((x, y), noise(random.choice(DEBRIS), 15))
save(i, "atik_alani")

scaffold("atik_alani")
model_cube_all("atik_alani")

# ── geri_donusum_istasyonu ────────────────────────────────────────────────────
GRD_GRN = (50, 160, 70)
GRD_WHT = (230, 232, 235)

i = fill(GRD_WHT, 8)
for y in range(16):
    for x in range(16):
        if x == 0 or x == 15 or y == 0 or y == 15:
            i.putpixel((x, y), noise(GRD_GRN, 8))
save(i, "geri_donusum_istasyonu_side")
save(recycle_symbol((50, 160, 70), (230, 232, 235)), "geri_donusum_istasyonu_top")

scaffold("geri_donusum_istasyonu")
model_cube("geri_donusum_istasyonu",
    f"{MOD}:block/geri_donusum_istasyonu_top",
    f"{MOD}:block/geri_donusum_istasyonu_side",
    f"{MOD}:block/geri_donusum_istasyonu_side",
    f"{MOD}:block/geri_donusum_istasyonu_side")

# ── duz_cati ──────────────────────────────────────────────────────────────────
save(fill((155, 153, 150), 12), "duz_cati")
scaffold("duz_cati")
model_cube_all("duz_cati")

# ── gunes_panel_cati ──────────────────────────────────────────────────────────
scaffold("gunes_panel_cati")
model_flat_roof_panel("gunes_panel_cati",
    f"{MOD}:block/temiz_jenerator_panel",   # güneş paneli texturesını yeniden kullan
    f"{MOD}:block/duz_cati")

# ── kuru_zemin ────────────────────────────────────────────────────────────────
save(cracked((160, 130, 85), (100, 75, 40), 20), "kuru_zemin")
scaffold("kuru_zemin")
model_cube_all("kuru_zemin")

# ── yagmur_suyu_tanki ─────────────────────────────────────────────────────────
TANK_BLUE = (50, 110, 175)

i = img()
for y in range(16):
    for x in range(16):
        if y % 6 == 5:
            c = noise((35, 85, 140), 8)
        elif x == 0 or x == 15:
            c = noise((180, 185, 190), 8)
        else:
            c = noise(TANK_BLUE, 12)
        i.putpixel((x, y), c)
save(i, "yagmur_suyu_tanki_side")

i = img()
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        c = (150, 220, 255) if (dx**2+dy**2)**0.5 < 5 else noise(TANK_BLUE, 10)
        i.putpixel((x, y), c)
save(i, "yagmur_suyu_tanki_top")

scaffold("yagmur_suyu_tanki")
model_cube("yagmur_suyu_tanki",
    f"{MOD}:block/yagmur_suyu_tanki_top",
    f"{MOD}:block/yagmur_suyu_tanki_side",
    f"{MOD}:block/yagmur_suyu_tanki_side",
    f"{MOD}:block/yagmur_suyu_tanki_side")

# ══════════════════════════════════════════════════════════════════════════════
#  LANG DOSYASI
# ══════════════════════════════════════════════════════════════════════════════
lang = {
    # Aşama 1
    "block.bsb-airquest.kirli_baca":           "Kirli Fabrika Bacası",
    "block.bsb-airquest.temiz_baca":           "Filtrelenmiş Baca",
    "block.bsb-airquest.dizel_jenerator":      "Dizel Jeneratör",
    "block.bsb-airquest.temiz_jenerator":      "Güneş Enerjili Jeneratör",
    "block.bsb-airquest.kirli_su_tanki":       "Kirli Su Tankı",
    "block.bsb-airquest.aritma_tanki":         "Su Arıtma Tankı",
    "block.bsb-airquest.sogutma_bloku":        "Endüstriyel Soğutma Bloku",
    "block.bsb-airquest.isi_esanjoru":         "Isı Eşanjörü",
    # Aşama 2
    "block.bsb-airquest.corak_toprak":         "Çorak Toprak",
    "block.bsb-airquest.yesil_alan":           "Yeşil Alan",
    "block.bsb-airquest.bozuk_asfalt":         "Bozuk Asfalt",
    "block.bsb-airquest.bisiklet_seridi":      "Bisiklet Şeridi",
    "block.bsb-airquest.beton_cati":           "Beton Çatı",
    "block.bsb-airquest.yesil_cati":           "Yeşil Çatı",
    "block.bsb-airquest.benzin_istasyonu":     "Benzin İstasyonu",
    "block.bsb-airquest.ev_sarj_istasyonu":    "EV Şarj İstasyonu",
    # Aşama 3
    "block.bsb-airquest.atik_alani":           "Atık Alanı",
    "block.bsb-airquest.geri_donusum_istasyonu": "Geri Dönüşüm İstasyonu",
    "block.bsb-airquest.duz_cati":             "Düz Çatı",
    "block.bsb-airquest.gunes_panel_cati":     "Güneş Panelli Çatı",
    "block.bsb-airquest.kuru_zemin":           "Kuru Zemin",
    "block.bsb-airquest.yagmur_suyu_tanki":   "Yağmur Suyu Tankı",
}
write_json(f"{LANG}/tr_tr.json", lang)
write_json(f"{LANG}/en_us.json", {k: v for k, v in lang.items()})
print("  lang tr_tr.json + en_us.json")

print(f"\n✓ Tamamlandı! {len(lang)} blok tanımı, tüm JSON'lar ve texturalar üretildi.")
