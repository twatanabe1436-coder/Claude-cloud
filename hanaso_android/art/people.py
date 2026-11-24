"""会話の相手 (17 人) と、表情 3 種 (normal / happy / puzzled)。

人物は「頭の中心 (360, 208)、半径 60」の座標で描き、200x200 の画像に縮めて書き出す。
肩から下は画像の下端 (この座標で y=356) までなので、場面ではカウンターや机で隠す。
"""

from dataclasses import dataclass, field

from vector import G, P, circle, ellipse, poly, rect, star4

MOODS = ("normal", "happy", "puzzled")

# 肌の色: (顔, 首・影, 鼻の線, ほお)
SKIN = {
    "light": ("#F8D2B0", "#EDB78C", "#D99B74", "#F59A8F"),
    "fair": ("#FCE1CC", "#F0C6A8", "#DDA585", "#F59A8F"),
    "tan": ("#E8B489", "#D59A6D", "#BF7C52", "#E8806F"),
    "brown": ("#C08457", "#A86E45", "#8E5A36", "#D9705F"),
    "dark": ("#8D5B3D", "#774A30", "#5C3823", "#C4604F"),
}

INK = "#2D2A32"
BROW = "#4A3326"
MOUTH = "#A9403F"
TONGUE = "#F07C7C"
GOLD = "#FFC83D"
ORANGE = "#F0A93B"

BODY = "M270 360C274 306 308 282 360 282C412 282 446 306 450 360Z"


@dataclass
class Person:
    id: str
    skin: str
    hair: str
    hair_color: str
    top: str
    c: dict = field(default_factory=dict)  # 服の色
    acc: tuple = ()  # 小物
    annoyed: bool = False  # ふだんの顔が不満そう (クレームの電話)


PEOPLE = [
    Person("jamie", "light", "bob", "#6B4226", "apron", {"main": "#DCE8F8", "apron": "#2E9C76"}, ("nametag",)),
    Person("pat", "brown", "curly", "#2B2321", "jacket", {"main": "#4A6FA5", "shade": "#3B5A88", "tee": "#F4C95D", "tee_shade": "#E0B243"}, ("glasses",)),
    Person("marco", "tan", "slick", "#2E2018", "vest", {"main": "#FAFAFA", "vest": "#2F2F38", "tie": "#C0392B"}, ("mustache",)),
    Person("reed", "fair", "buzz", "#8C6A4A", "uniform", {"main": "#2C3E66", "shade": "#22314F"}, ()),
    Person("olivia", "light", "bun", "#D9A441", "blazer", {"main": "#8E2B4A", "lapel": "#74203C", "inner": "#FFF6E8", "scarf": "#F2C14E"}, ("nametag", "earrings")),
    Person("sam", "fair", "ponytail", "#A0522D", "striped", {"main": "#FFFFFF", "stripe": "#3D5A99", "lanyard": "#E4572E"}, ()),
    Person("chris", "light", "messy", "#C49A5A", "jacket", {"main": "#5B7DB1", "shade": "#4A6A9C", "tee": "#F7B6C6", "tee_shade": "#E79CAE"}, ("earring", "drink")),
    Person("taylor", "dark", "long", "#1F1A17", "hoodie", {"main": "#B9A6F0", "shade": "#9F89E0"}, ("phone",)),
    Person("drlee", "fair", "pixie", "#1E1B1A", "coat", {"main": "#FFFFFF", "inner": "#3FA7A0"}, ("glasses", "stethoscope")),
    Person("jordan", "light", "short", "#5A3E2B", "flannel", {"main": "#C0392B", "shade": "#8E2A20"}, ("beard", "mug")),
    Person("morgan", "tan", "ponytail", "#3B2A20", "cardigan", {"main": "#E3A93B", "shade": "#C98E26", "inner": "#FFFFFF"}, ("earrings",)),
    Person("carter", "dark", "bun", "#1A1716", "blazer", {"main": "#26355E", "lapel": "#1C2847", "inner": "#FFFFFF"}, ("earrings",)),
    Person("dana", "light", "pixie", "#8B3A2B", "blazer", {"main": "#6B7280", "lapel": "#565C68", "inner": "#CFE3F7"}, ()),
    Person("casey", "brown", "short", "#1B1716", "sweater", {"main": "#5C7A99", "shade": "#4A6580"}, ("phone",), annoyed=True),
    Person("riley", "fair", "messy", "#22201E", "sweater", {"main": "#2F7F86", "shade": "#246469"}, ("glasses",)),
    Person("patel", "brown", "short", "#1E1A18", "blazer", {"main": "#3D4250", "lapel": "#2F333F", "inner": "#E8EEF6"}, ("beard", "glasses", "mic")),
    Person("alex", "tan", "curly", "#6A4A32", "jacket", {"main": "#5E7D4A", "shade": "#4D6A3C", "tee": "#F3EDE2", "tee_shade": "#DED5C6", "scarf": "#E07A3F"}, ("scarf",)),
]


# ---- 髪 (後ろ・肩にかかる分・前) ----

def hair(p):
    c = p.hair_color
    hi = _shade(c, 1.35)
    back, over, front = [], [], []
    h = p.hair
    if h == "bob":
        back.append(P("M296 206C292 152 326 130 360 130C394 130 428 152 424 206L426 258C418 268 404 266 400 256L320 256C316 266 302 268 294 258Z", c))
        front.append(P("M300 212C298 160 330 140 360 140C392 140 424 158 422 210C414 186 398 172 380 168C368 182 344 188 322 180C312 190 304 200 300 212Z", c))
        front.append(P("M332 156Q350 148 372 152", None, hi, 5))
    elif h == "short":
        front.append(P("M300 216C294 160 326 136 362 136C398 136 428 160 420 216C416 196 408 184 396 178C378 188 348 188 328 180C316 188 306 200 300 216Z", c))
        front.append(P("M334 150Q356 142 384 148", None, hi, 4))
    elif h == "slick":
        front.append(P("M300 214C296 160 328 138 362 138C398 138 426 160 420 214C416 186 402 170 372 166C344 164 316 176 300 214Z", c))
        front.append(P("M326 158Q356 144 394 152", None, hi, 4))
    elif h == "buzz":
        front.append(P("M302 204C302 162 330 142 360 142C390 142 418 162 418 204C410 182 390 170 360 170C330 170 310 182 302 204Z", c))
    elif h == "bun":
        back.append(P(circle(360, 132, 26), c))
        back.append(P("M342 150Q360 157 378 150", None, hi, 4))
        front.append(P("M300 214C298 162 328 142 360 142C392 142 422 162 420 214C414 186 396 170 360 168C324 170 306 186 300 214Z", c))
        front.append(P("M360 146V166", None, _shade(c, 0.8), 2.5))
    elif h == "ponytail":
        back.append(P("M404 160C450 168 460 236 432 280C426 252 422 222 408 198Z", c))
        front.append(P("M300 214C298 162 328 142 360 142C392 142 422 162 420 214C414 186 396 170 360 168C324 170 306 186 300 214Z", c))
        front.append(P(circle(410, 164, 6), "#E4572E"))
        front.append(P("M334 152Q356 144 380 148", None, hi, 4))
    elif h == "long":
        back.append(P("M292 210C288 150 326 128 360 128C394 128 432 150 428 210L430 306C418 318 404 312 402 300L318 300C316 312 302 318 290 306Z", c))
        over.append(P("M298 220C290 256 292 296 300 318C310 322 320 312 320 300C316 274 314 246 312 228Z", c))
        over.append(P("M422 220C430 256 428 296 420 318C410 322 400 312 400 300C404 274 406 246 408 228Z", c))
        front.append(P("M300 214C298 160 330 140 360 140C390 140 422 160 420 214C412 186 394 168 362 164C330 168 308 186 300 214Z", c))
        front.append(P("M330 156Q348 148 362 150", None, hi, 4))
    elif h == "curly":
        for x, y, r in ((304, 196, 20), (314, 168, 22), (336, 148, 22), (360, 140, 22), (384, 148, 22), (406, 168, 22), (416, 196, 20), (300, 224, 15), (420, 224, 15)):
            back.append(P(circle(x, y, r), c))
        front.append(P("M304 204C304 156 334 136 360 136C386 136 416 156 416 204C406 186 390 176 360 176C330 176 314 186 304 204Z", c))
        for x, y, r in ((320, 182, 15), (340, 170, 15), (360, 166, 15), (380, 170, 15), (400, 182, 15)):
            front.append(P(circle(x, y, r), c))
        front.append(P("M336 152Q346 146 356 150M368 150Q378 146 388 152", None, hi, 3.5))
    elif h == "pixie":
        back.append(P("M302 226C296 240 298 254 306 260L314 232Z", c))
        back.append(P("M418 226C424 240 422 254 414 260L406 232Z", c))
        front.append(P("M300 222C294 158 330 136 364 136C400 136 428 162 420 222C416 200 412 186 402 180C380 196 340 194 310 170C304 186 302 202 300 222Z", c))
        front.append(P("M344 150Q366 142 392 152", None, hi, 4))
    elif h == "messy":
        front.append(P("M300 212L302 172L314 178L318 150L338 164L350 136L366 158L384 138L392 164L410 152L412 180L422 184L420 212C410 190 392 180 360 178C328 180 310 190 300 212Z", c))
    return back, over, front


def _shade(hex_color, k):
    """色を明るく (k>1) / 暗く (k<1)"""
    r, g, b = (int(hex_color[i:i + 2], 16) for i in (1, 3, 5))
    if k >= 1:
        f = k - 1
        r, g, b = (int(v + (255 - v) * f * 0.5) for v in (r, g, b))
    else:
        r, g, b = (int(v * k) for v in (r, g, b))
    return f"#{min(r, 255):02X}{min(g, 255):02X}{min(b, 255):02X}"


# ---- 服 ----

def clothes(p):
    c = p.c
    main = c["main"]
    out = [P(BODY, main)]
    t = p.top
    if t == "apron":
        out += [
            P("M326 290H394L400 360H320Z", c["apron"]),
            P("M331 292L345 268M389 292L375 268", None, c["apron"], 6),
        ]
    elif t == "vest":
        out += [
            P(BODY, None, "#E2E2E2", 1.5),
            P(poly((346, 282), (360, 298), (340, 302)), "#FFFFFF", "#DDDDDD", 1.5),
            P(poly((374, 282), (360, 298), (380, 302)), "#FFFFFF", "#DDDDDD", 1.5),
            P("M272 360C276 312 300 292 330 287L352 334L350 360Z", c["vest"]),
            P("M448 360C444 312 420 292 390 287L368 334L370 360Z", c["vest"]),
            P(poly((343, 290), (360, 297), (377, 290), (377, 306), (360, 299), (343, 306)), c["tie"]),
            P(circle(360, 298, 4.5), _shade(c["tie"], 0.8)),
        ]
    elif t == "uniform":
        sh = c["shade"]
        out += [
            P(poly((344, 282), (360, 300), (338, 305)), sh),
            P(poly((376, 282), (360, 300), (382, 305)), sh),
            P("M360 300V360", None, sh, 2),
            P(circle(360, 318, 2.6), "#C9D3E6"),
            P(circle(360, 340, 2.6), "#C9D3E6"),
            P("M292 302L330 289L333 297L296 310Z", sh),
            P("M428 302L390 289L387 297L424 310Z", sh),
            P("M312 312H334V324C334 334 323 340 323 340C323 340 312 334 312 324Z", "#E7B93E"),
            P(circle(323, 323, 3.5), "#FFF3C4"),
            P(rect(386, 316, 26, 7, 2), "#C9D3E6"),
        ]
    elif t == "blazer":
        out += [
            P(poly((336, 283), (360, 340), (384, 283)), c["inner"]),
            P(poly((336, 283), (322, 297), (340, 313), (330, 323), (360, 342)), c["lapel"]),
            P(poly((384, 283), (398, 297), (380, 313), (390, 323), (360, 342)), c["lapel"]),
            P(circle(360, 352, 3), c["lapel"]),
        ]
        if "scarf" in c:
            out += [
                P(poly((344, 285), (376, 285), (360, 305)), c["scarf"]),
                P(poly((360, 300), (373, 322), (357, 318)), _shade(c["scarf"], 0.85)),
            ]
    elif t == "coat":
        out += [
            P(BODY, None, "#D8DFE7", 1.5),
            P(poly((338, 283), (360, 332), (382, 283)), c["inner"]),
            P(poly((338, 283), (325, 300), (343, 316), (360, 342)), "#EEF2F6"),
            P(poly((382, 283), (395, 300), (377, 316), (360, 342)), "#EEF2F6"),
            P(rect(304, 330, 28, 22, 3), "#EEF2F6"),
            P(rect(311, 323, 4, 12, 2), "#3D6FD9"),
        ]
    elif t == "hoodie":
        sh = c["shade"]
        out += [
            P("M314 286C324 266 396 266 406 286C396 302 324 302 314 286Z", sh),
            P("M350 297L347 332M370 297L373 332", None, "#FFFFFF", 3),
            P(circle(347, 334, 3), "#FFFFFF"),
            P(circle(373, 334, 3), "#FFFFFF"),
        ]
    elif t == "flannel":
        sh = c["shade"]
        checks = [P(rect(x, 280, 9, 80), sh, alpha=0.45) for x in range(276, 450, 26)]
        checks += [P(rect(268, y, 184, 8), sh, alpha=0.35) for y in range(292, 360, 22)]
        out += [
            G(checks, clip=BODY),
            P(poly((346, 284), (360, 300), (374, 284)), "#F2F2F2"),
            P(poly((342, 282), (360, 299), (334, 306)), sh),
            P(poly((378, 282), (360, 299), (386, 306)), sh),
        ]
    elif t == "jacket":
        sh = c["shade"]
        out += [
            P("M330 286C340 300 380 300 390 286L392 360H328Z", c["tee"]),
            P("M336 287C344 297 376 297 384 287", None, c["tee_shade"], 3),
            P("M330 286L326 360M390 286L394 360", None, sh, 3),
            P(poly((331, 285), (314, 300), (327, 313)), sh),
            P(poly((389, 285), (406, 300), (393, 313)), sh),
        ]
    elif t == "sweater":
        sh = c["shade"]
        out += [
            P(poly((344, 282), (360, 296), (338, 301)), "#FFFFFF"),
            P(poly((376, 282), (360, 296), (382, 301)), "#FFFFFF"),
            P("M336 285C346 302 374 302 384 285", None, sh, 4),
        ]
    elif t == "cardigan":
        sh = c["shade"]
        out += [
            P("M334 285C342 300 378 300 386 285L382 360H338Z", c["inner"]),
            P("M334 285L338 360M386 285L382 360", None, sh, 3),
            P(circle(345, 318, 3), sh),
            P(circle(346, 342, 3), sh),
        ]
    elif t == "striped":
        stripes = [P(rect(268, y, 184, 6), c["stripe"], alpha=0.85) for y in range(288, 360, 14)]
        out += [
            G(stripes, clip=BODY),
            P(poly((344, 282), (360, 296), (338, 302)), "#FFFFFF", c["stripe"], 1.5),
            P(poly((376, 282), (360, 296), (382, 302)), "#FFFFFF", c["stripe"], 1.5),
            P("M346 287L356 330M374 287L364 330", None, c["lanyard"], 3),
            P(rect(349, 328, 22, 27, 3), "#FFFFFF", "#C9CED6", 1.5),
            P(rect(349, 328, 22, 7, 2), c["lanyard"]),
        ]
    return out


# ---- 顔 ----

def face(p, mood):
    skin, neck, nose, blush = SKIN[p.skin]
    out = [
        P(ellipse(324, 234, 10, 6), blush, alpha=0.5),
        P(ellipse(396, 234, 10, 6), blush, alpha=0.5),
        P("M358 222Q360 228 364 224", None, nose, 3),
    ]
    eyes = [
        P(ellipse(338, 214, 6, 8), INK),
        P(ellipse(382, 214, 6, 8), INK),
        P(circle(340, 211, 2.2), "#FFFFFF"),
        P(circle(384, 211, 2.2), "#FFFFFF"),
    ]
    if mood == "happy":
        out.append(P("M330 192Q338 186 346 191M374 191Q382 186 390 192", None, BROW, 4))
        out.append(P("M330 217Q338 206 346 217M374 217Q382 206 390 217", None, INK, 4.5))
        if p.annoyed:
            out.append(P("M348 242Q360 250 372 242", None, MOUTH, 4))
        else:
            out.append(P("M345 237Q360 241 375 237Q372 254 360 255Q348 254 345 237Z", MOUTH))
            out.append(P("M352 249Q360 244 368 249Q364 254 360 254Q356 254 352 249Z", TONGUE))
    elif mood == "puzzled":
        out.append(P("M330 196Q338 194 346 199M374 190Q382 184 390 189", None, BROW, 4))
        out += eyes
        out.append(P(ellipse(362, 246, 6, 7), MOUTH))
    elif p.annoyed:
        out.append(P("M330 193L346 199M390 193L374 199", None, BROW, 4))
        out += eyes
        out.append(P("M349 247Q360 242 371 247", None, MOUTH, 4))
    else:
        out.append(P("M330 196Q338 191 346 195M374 195Q382 191 390 196", None, BROW, 4))
        out += eyes
        out.append(P("M348 241Q360 250 372 241", None, MOUTH, 4))
    return out


def glasses():
    return [
        P(circle(338, 214, 15), "#FFFFFF", "#2F2F38", 3.5, alpha=0.18, stroke_alpha=1),
        P(circle(382, 214, 15), "#FFFFFF", "#2F2F38", 3.5, alpha=0.18, stroke_alpha=1),
        P("M353 212Q360 207 367 212M323 210L303 205M397 210L417 205", None, "#2F2F38", 3),
    ]


def beard(color):
    return P("M304 214C306 262 334 290 360 290C386 290 414 262 416 214C410 236 400 248 384 254C376 246 344 246 336 254C320 248 310 236 304 214Z", color)


def mustache(color):
    return P("M340 236Q350 228 360 234Q370 228 380 236Q372 244 360 240Q348 244 340 236Z", color)


# ---- 手に持つもの ----

def props(p):
    skin = SKIN[p.skin][0]
    sleeve = p.c["main"]
    out = []
    if "phone" in p.acc:
        out += [
            P("M434 360C442 322 446 292 432 266", None, sleeve, 24),
            P(rect(413, 194, 19, 42, 5), "#2D2F3A"),
            P(rect(416, 199, 13, 32, 3), "#5B6375"),
            P(circle(425, 251, 15), skin),
        ]
    if "mic" in p.acc:
        out += [
            P("M418 360C420 334 416 314 406 300", None, sleeve, 24),
            P("M394 258L402 298", None, "#2D2F3A", 10),
            P(circle(393, 247, 12), "#4A4F5C"),
            P("M385 243H401M385 250H401", None, "#6D7383", 2),
            P(circle(402, 292, 14), skin),
        ]
    if "mug" in p.acc:
        out += [
            P("M432 360C436 332 428 314 412 306", None, sleeve, 24),
            P("M384 278C380 268 390 264 386 254M398 278C394 268 404 264 400 254", None, "#FFFFFF", 3, alpha=0.9),
            P(rect(372, 286, 36, 40, 7), "#F4F1EA"),
            P(rect(372, 300, 36, 9), "#6C5CE7"),
            P("M408 296A10 10 0 0 1 408 316", None, "#F4F1EA", 6),
            P(circle(410, 318, 13), skin),
        ]
    if "drink" in p.acc:
        out += [
            P("M432 360C436 334 428 318 412 312", None, sleeve, 24),
            P("M394 262L402 296", None, "#4ECDC4", 3),
            P(poly((378, 280), (408, 280), (402, 328), (384, 328)), "#FFFFFF", "#BFD3E6", 2, alpha=0.75, stroke_alpha=1),
            P(poly((381, 296), (405, 296), (402, 328), (384, 328)), "#F2A541"),
            P(circle(408, 322, 13), skin),
        ]
    return out


def character(p, mood):
    skin, neck, _, _ = SKIN[p.skin]
    back, over, front = hair(p)
    nodes = list(back)
    nodes.append(P(rect(346, 248, 28, 42, 10), neck))
    nodes += clothes(p)
    if "stethoscope" in p.acc:
        nodes += [
            P("M338 288C330 316 336 338 350 344M382 288C390 312 386 330 376 338", None, "#4A5568", 4),
            P(circle(372, 342, 7), "#B8C2CC", "#4A5568", 2),
        ]
    if "nametag" in p.acc:
        nodes.append(P(rect(382, 312, 22, 10, 2), "#FFFFFF", "#D8D8D8", 1))
    if "scarf" in p.acc:
        sc = p.c["scarf"]
        nodes += [
            P("M322 276C338 300 382 300 398 276L404 292C386 316 334 316 316 292Z", sc),
            P(poly((376, 300), (394, 342), (378, 347), (364, 306)), _shade(sc, 0.82)),
        ]
    nodes += over
    nodes.append(P(circle(360, 208, 60), skin))
    if "earrings" in p.acc:
        nodes += [P(circle(301, 244, 4.5), GOLD), P(circle(419, 244, 4.5), GOLD)]
    if "earring" in p.acc:
        nodes.append(P(circle(301, 246, 4), "#C9D3E6"))
    if "beard" in p.acc:
        nodes.append(beard(p.hair_color))
    if "mustache" in p.acc:
        nodes.append(mustache(p.hair_color))
    nodes += front
    nodes += face(p, mood)
    if "glasses" in p.acc:
        nodes += glasses()
    nodes += props(p)
    if mood == "happy":
        nodes += [P(star4(300, 146, 14), GOLD), P(star4(436, 156, 10), GOLD)]
    elif mood == "puzzled":
        nodes += [
            P(circle(430, 150, 18), "#FFFFFF", ORANGE, 3),
            P("M424 145Q424 138 430 138Q436 138 436 144Q436 148 432 150Q430 151 430 155", None, ORANGE, 3.6),
            P(circle(430, 161, 2.2), ORANGE),
        ]
    return nodes


# 人物の座標 → 200x200 の画像
K = 46 / 60
CANVAS = 200


def character_group(p, mood):
    return G(character(p, mood), tx=100 - 360 * K, ty=86 - 208 * K, s=K)
