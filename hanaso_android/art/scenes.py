"""会話の場面 (17 か所) の背景と前景。

場面は 720x400。人物は (230, 96) から 260x260 の大きさで、背景と前景のあいだに置く。
人物の体の下端 (y=356) が見えないよう、前景 (カウンター・机・ビデオ通話の操作バーなど) は y=332 より上から始める。
真ん中 (x 280〜440, y 120〜330) は人物が立つので、背景の小物は置かない。
"""

from vector import G, P, circle, ellipse, poly, rect, star4, vgrad

W, H = 720, 400
CHAR_X, CHAR_Y, CHAR_SIZE = 230, 96, 260
WHITE = "#FFFFFF"


# ---- 共通の部品 ----

def wall(top, bottom):
    return P(rect(0, 0, W, H), vgrad(0, H, top, bottom))


def window(x, y, w, h, sky, view=(), frame=WHITE, cross=True):
    out = [P(rect(x - 6, y - 6, w + 12, h + 12, 16), frame)]
    out.append(G(P(rect(x, y, w, h), vgrad(y, y + h, *sky)), list(view), clip=rect(x, y, w, h, 11)))
    if cross:
        out.append(P(f"M{x + w / 2} {y}V{y + h}M{x} {y + h / 2}H{x + w}", None, frame, 6, cap="butt"))
    return out


def lamp(x, length=60, shade="#F0A93B", rim="#D38A22", glow="#FFE9A8"):
    y = length
    return [
        P(f"M{x} 0V{y}", None, "#5B4636", 3),
        P(circle(x, y + 26, 34), glow, alpha=0.35),
        P(f"M{x - 24} {y + 20}A24 22 0 0 1 {x + 24} {y + 20}Z", shade),
        P(ellipse(x, y + 20, 24, 4), rim),
    ]


def plant(x, base, s=1.0, pot="#E07A5F", leaves=("#4FA36F", "#3E8F5E", "#5BB57C")):
    k = s
    return [
        P(ellipse(x - 14 * k, base - 56 * k, 10 * k, 24 * k, -28), leaves[0]),
        P(ellipse(x + 14 * k, base - 56 * k, 10 * k, 24 * k, 28), leaves[1]),
        P(ellipse(x, base - 66 * k, 10 * k, 26 * k), leaves[2]),
        P(poly((x - 22 * k, base - 36 * k), (x + 22 * k, base - 36 * k), (x + 16 * k, base), (x - 16 * k, base)), pot),
    ]


def picture(x, y, w, h, frame="#9B7350", sky=("#BFE3FA", "#EAF6FF"), hills=("#8CC7A1", "#6FB38A")):
    return [
        P(rect(x - 6, y - 6, w + 12, h + 12, 6), frame),
        G(
            P(rect(x, y, w, h), vgrad(y, y + h, *sky)),
            P(f"M{x} {y + h}L{x + w * 0.35} {y + h * 0.45}L{x + w * 0.6} {y + h * 0.75}L{x + w * 0.8} {y + h * 0.5}L{x + w} {y + h * 0.8}V{y + h}Z", hills[1]),
            P(f"M{x} {y + h}C{x + w * 0.3} {y + h * 0.7} {x + w * 0.7} {y + h * 0.9} {x + w} {y + h * 0.75}V{y + h}Z", hills[0]),
            P(circle(x + w * 0.78, y + h * 0.28, h * 0.12), "#FFE7A8"),
            clip=rect(x, y, w, h),
        ),
    ]


def books(x, base, colors, w=14):
    out = []
    for i, c in enumerate(colors):
        hgt = 40 + (i * 7) % 16
        out.append(P(rect(x + i * (w + 3), base - hgt, w, hgt, 2), c))
    return out


def shelf(x, y, w, colors):
    return [*books(x + 8, y, colors), P(rect(x, y, w, 8, 2), "#9B7350")]


def counter(top=318, lip="#D9A57A", surface="#C48D5F", front="#9A6541", line="#87583A", planks=True):
    out = [P(rect(0, top, W, 16), surface), P(rect(0, top, W, 3), lip), P(rect(0, top + 16, W, H - top - 16), front)]
    if planks:
        out.append(P("".join(f"M{x} {top + 16}V{H}" for x in range(90, W, 120)), None, line, 3, cap="butt"))
    return out


def string_lights(y0=36, sag=46, colors=("#FFD166", "#FF7AA2", "#7FDBDA", "#B39DFF")):
    out = [P(f"M0 {y0}Q{W / 2} {y0 + sag * 2} {W} {y0}", None, "#3B3355", 2)]
    for i in range(1, 12):
        t = i / 12
        x = W * t
        y = (1 - t) ** 2 * y0 + 2 * (1 - t) * t * (y0 + sag * 2) + t ** 2 * y0
        c = colors[i % len(colors)]
        out += [P(circle(x, y + 8, 12), c, alpha=0.3), P(ellipse(x, y + 8, 5, 7), c)]
    return out


def mug(x, base, color="#FFFFFF", band=None):
    out = [P(rect(x, base - 30, 28, 30, 6), color), P(f"M{x + 28} {base - 24}A8 8 0 0 1 {x + 28} {base - 8}", None, color, 5)]
    if band:
        out.append(P(rect(x, base - 20, 28, 7), band))
    return out


def video_ui():
    """ビデオ通話の画面 (下の操作バー・自分の小窓・名前の札)"""
    return [
        P(rect(0, 332, W, 68), "#1E2230"),
        P(rect(0, 332, W, 2), "#2E3446"),
        P(circle(300, 366, 20), "#3A4157"),
        P(rect(294, 354, 12, 18, 6), WHITE),
        P(f"M290 368A10 10 0 0 0 310 368M300 378V382", None, WHITE, 2.5),
        P(circle(360, 366, 20), "#3A4157"),
        P(rect(346, 358, 20, 16, 4), WHITE),
        P(poly((366, 362), (376, 357), (376, 375), (366, 370)), WHITE),
        P(rect(400, 352, 64, 28, 14), "#E5484D"),
        P("M416 368Q432 356 448 368", None, WHITE, 4),
        P(rect(560, 222, 136, 96, 12), "#2B3042"),
        P(circle(628, 262, 17), "#9C8CF0"),
        P("M598 318C600 296 612 286 628 286C644 286 656 296 658 318Z", "#7B68EE"),
        P(rect(560, 222, 136, 96, 12), None, "#4A5270", 3),
        P(rect(22, 290, 118, 28, 14), "#1E2230", alpha=0.75),
        P(circle(40, 304, 7), "#46C37B"),
        P(rect(54, 300, 72, 8, 4), WHITE, alpha=0.85),
    ]


def skyline(base, x0, x1, color, seed=3):
    out = []
    x = x0
    i = seed
    while x < x1:
        w = 18 + (i * 13) % 22
        h = 30 + (i * 29) % 60
        out.append(P(rect(x, base - h, w, h), color))
        x += w + 2
        i += 1
    return out


def cloud(x, y, s=1.0, c=WHITE, a=0.9):
    return [P(ellipse(x, y, 30 * s, 14 * s), c, alpha=a), P(ellipse(x + 22 * s, y - 8 * s, 22 * s, 14 * s), c, alpha=a), P(ellipse(x - 20 * s, y - 4 * s, 16 * s, 10 * s), c, alpha=a)]


def confetti_bits(area, n=28, seed=7):
    x0, y0, x1, y1 = area
    colors = ("#FF7AA2", "#FFD166", "#4ECDC4", "#7B68EE", "#F28C28")
    out = []
    for i in range(n):
        seed = (seed * 1103515245 + 12345) % 2 ** 31
        x = x0 + seed % int(x1 - x0)
        seed = (seed * 1103515245 + 12345) % 2 ** 31
        y = y0 + seed % int(y1 - y0)
        c = colors[i % len(colors)]
        if i % 3 == 0:
            out.append(P(circle(x, y, 5), c))
        else:
            out.append(G(P(rect(x - 7, y - 3, 14, 6, 2), c), rot=(i * 37) % 180, px=x, py=y))
    return out


# ---- 場面 ----

def cafe():
    bg = [
        wall("#FBEFDD", "#F2DCC2"),
        window(40, 60, 170, 150, ("#BFE3FA", "#EAF6FF"), [
            P(circle(80, 96, 16), "#FFE7A8"),
            P("M40 210V172H60V152H82V178H98V144H120V170H136V162H150V210Z", "#A8CBE6"),
            P("M174 210V168H194V184H210V210Z", "#A8CBE6"),
            P("M156 210L161 130L165 130L170 210Z", "#8DB6D8"),
            P(ellipse(163, 128, 17, 5), "#8DB6D8"),
            P(poly((162, 123), (163, 108), (164, 123)), "#8DB6D8"),
        ]),
        P(rect(462, 44, 216, 140, 14), "#9B7350"),
        P(rect(472, 54, 196, 120, 8), "#3F4B47"),
        P(rect(492, 70, 70, 8, 4), "#F7F3E8", alpha=0.9),
        P("M598 66H620V76A11 11 0 0 1 598 76ZM620 70A5 5 0 0 1 620 80", None, "#F7F3E8", 3, alpha=0.9),
        [P(rect(492, y, wd, 6, 3), "#F7F3E8", alpha=0.55) for y, wd in ((96, 96), (114, 80), (132, 104), (150, 70))],
        [P(rect(624, y, 26, 6, 3), "#F7F3E8", alpha=0.55) for y in (96, 114, 132, 150)],
        lamp(276), lamp(444),
    ]
    fg = [
        counter(),
        P(rect(118, 226, 122, 16, 6), "#8E98A6"),
        P(rect(124, 238, 110, 80, 10), "#C3CAD4"),
        P(rect(160, 262, 38, 12, 4), "#6E7785"),
        P("M170 274h4v8h-4zM184 274h4v8h-4z", "#6E7785"),
        P("M166 294H192V304A6 6 0 0 1 186 310H172A6 6 0 0 1 166 304Z", WHITE),
        P(rect(150, 311, 58, 7, 3), "#6E7785"),
        P(circle(214, 256, 9), WHITE, "#6E7785", 3),
        P("M214 256L219 251", None, "#E0574F", 2.5),
        P("M540 246C532 234 548 228 540 214M556 246C548 234 564 228 556 214", None, WHITE, 4),
        P(poly((528, 262), (568, 262), (562, 318), (534, 318)), WHITE),
        P(poly((531, 280), (565, 280), (563, 300), (533, 300)), "#D9A47C"),
        P(rect(524, 252, 48, 12, 5), "#6B4E3D"),
        plant(654, 318),
    ]
    return bg, fg


def directions():
    bg = [
        P(rect(0, 0, W, H), vgrad(0, 330, "#8ED0F2", "#DFF3FC")),
        P(circle(612, 66, 44), "#FFF2B8", alpha=0.5),
        P(circle(612, 66, 28), "#FFE08A"),
        cloud(170, 70, 1.1), cloud(470, 54, 0.8),
        skyline(300, 210, 520, "#BCD5E8", seed=5),
        P("M262 300V96H286V70H300V96H312V300Z", "#AFC9DF"),
        P(rect(0, 92, 196, 308), "#C8714F"),
        [P(rect(18 + c * 44, 112 + r * 46, 26, 30, 3), "#F7E6C8", alpha=0.85) for c in range(4) for r in range(5)],
        P(rect(0, 92, 196, 10), "#A85A3D"),
        P(rect(524, 54, 196, 346), "#7FA9C9"),
        [P(rect(524, y, 196, 3), "#A9C8E0") for y in range(78, 330, 30)],
        [P(rect(x, 54, 3, 290), "#A9C8E0") for x in range(560, 720, 40)],
        P(circle(476, 244, 52), "#6BBF6B"),
        P(circle(500, 210, 38), "#7ACB78"),
        P(rect(470, 270, 12, 70), "#8A5A3B"),
        P(rect(226, 118, 8, 222), "#2F5D50"),
        P(rect(182, 140, 96, 24, 4), "#2E7D4F"),
        P(rect(194, 149, 60, 6, 3), WHITE, alpha=0.9),
        P(ellipse(230, 112, 16, 8), "#2F5D50"),
        P(circle(230, 122, 7), "#FFE8A3"),
    ]
    fg = [
        P(rect(0, 332, W, 68), "#D9D4CC"),
        P(rect(0, 332, W, 6), "#C2BCB2"),
        P("M0 372H720", None, "#C9C3B8", 2),
        P(rect(28, 296, 196, 42, 6), "#A86B45"),
        P(rect(28, 296, 196, 8, 4), "#C08259"),
        [P(circle(48 + i * 24, 290 - (i % 2) * 8, 11), c) for i, c in enumerate(("#FF7AA2", "#FFD166", "#FF7AA2", "#B39DFF", "#FFD166", "#FF7AA2", "#B39DFF", "#FFD166"))],
        [P(circle(44 + i * 24, 300, 9), "#4FA36F") for i in range(8)],
        P(rect(500, 296, 196, 42, 6), "#A86B45"),
        P(rect(500, 296, 196, 8, 4), "#C08259"),
        [P(ellipse(520 + i * 24, 282, 8, 16), "#5BB57C") for i in range(8)],
        [P(ellipse(520 + i * 24, 268, 7, 9), c) for i, c in enumerate(("#E4572E", "#F2C14E", "#E4572E", "#FF7AA2", "#F2C14E", "#E4572E", "#FF7AA2", "#F2C14E"))],
    ]
    return bg, fg


def restaurant():
    bg = [
        wall("#E9B48A", "#D9966A"),
        P(rect(0, 250, W, 150), "#7A4B32"),
        P(rect(0, 250, W, 8), "#93603F"),
        window(40, 56, 160, 150, ("#24305C", "#4A5A8C"), [
            P(circle(170, 90, 14), "#FFF3C4"),
            P(circle(176, 86, 12), "#2E3C6B"),
            [P(circle(x, y, 1.8), WHITE, alpha=0.8) for x, y in ((60, 80), (96, 70), (130, 100), (74, 120))],
            skyline(206, 40, 200, "#1B2447", seed=2),
            P("M108 206V120H124V206Z", "#1B2447"),
            P(poly((106, 120), (116, 96), (126, 120)), "#1B2447"),
            P(circle(116, 134, 6), "#FFE8A3"),
        ], frame="#F3E3CF"),
        picture(520, 62, 150, 96, frame="#5C3A24", sky=("#FFD9A8", "#FFF1DA"), hills=("#9CC48B", "#7BAA6F")),
        [P(rect(536 + i * 26, 190, 16, 50, 6), c) for i, c in enumerate(("#5B2333", "#2F5D3A", "#5B2333", "#2F5D3A", "#5B2333"))],
        P(rect(526, 238, 140, 8, 2), "#5C3A24"),
        P(circle(250, 116, 28), "#FFE9A8", alpha=0.4),
        P(poly((238, 104), (262, 104), (256, 126), (244, 126)), "#F0C27B"),
        P(circle(470, 116, 28), "#FFE9A8", alpha=0.4),
        P(poly((458, 104), (482, 104), (476, 126), (464, 126)), "#F0C27B"),
    ]
    checks = [P(rect(x, 320, 22, 80), "#D64545", alpha=0.55) for x in range(0, W, 44)]
    checks += [P(rect(0, y, W, 14), "#D64545", alpha=0.45) for y in range(330, 400, 28)]
    fg = [
        P(rect(0, 320, W, 80), WHITE),
        G(checks, clip=rect(0, 320, W, 80)),
        P(rect(0, 318, W, 5), "#F3E3CF"),
        P(ellipse(180, 320, 62, 15), WHITE, "#E5D5C3", 2),
        P(ellipse(180, 314, 38, 11), "#F2C14E"),
        P("M156 312Q170 304 184 312T206 312M160 318Q176 310 192 318", None, "#E3A92C", 2.5),
        P(circle(186, 308, 8), "#D64545"),
        P(poly((528, 318), (550, 318), (546, 258), (532, 258)), "#2F6B4F"),
        P(rect(533, 236, 12, 24, 3), "#FFF3D6"),
        P(circle(539, 228, 14), "#FFD27A", alpha=0.35),
        P(ellipse(539, 228, 5, 9), "#FFB347"),
        P("M606 318H634M620 318V284", None, "#DDE6EE", 3),
        P("M604 250H636C636 272 628 284 620 284C612 284 604 272 604 250Z", WHITE, "#DDE6EE", 2, alpha=0.6, stroke_alpha=1),
        P("M606 262H634C632 276 626 282 620 282C614 282 608 276 606 262Z", "#9B1D3A"),
    ]
    return bg, fg


def immigration():
    bg = [
        wall("#E6EEF6", "#CFDCEA"),
        window(36, 58, 190, 150, ("#A9D8F5", "#E3F4FD"), [
            P(rect(36, 176, 190, 40), "#9FB2C4"),
            P("M60 176V110M140 176V110", None, "#E0603F", 6),
            P("M36 150Q60 112 100 150Q140 112 180 150Q200 128 226 140", None, "#E0603F", 3),
            P("M36 156H226", None, "#E0603F", 5),
            G(
                P(ellipse(160, 88, 38, 7), WHITE),
                P(poly((128, 84), (140, 66), (148, 66), (142, 86)), "#3A6EA5"),
                P(poly((150, 90), (172, 108), (180, 108), (166, 88)), WHITE),
                rot=-8, px=160, py=88,
            ),
        ]),
        P("M534 0V38M616 0V38", None, "#8C9BAD", 3),
        P(rect(470, 38, 216, 60, 10), "#1F4E8C"),
        P(rect(496, 52, 26, 34, 4), WHITE),
        P(circle(509, 66, 7), "#1F4E8C"),
        P(rect(500, 78, 18, 3), "#1F4E8C"),
        P("M548 68H630", None, WHITE, 6),
        P(poly((626, 56), (648, 68), (626, 80)), WHITE),
        P(circle(586, 156, 30), WHITE, "#8C9BAD", 4),
        P("M586 156V136M586 156L600 164", None, "#2F3B4C", 3.5),
        [P(rect(x, 244, 8, 86, 3), "#9AA5B1") for x in (494, 590, 686)],
        [P(circle(x + 4, 242, 7), "#B7C1CC") for x in (494, 590, 686)],
        P("M502 262Q546 276 590 262M598 262Q642 276 686 262", None, "#3A6EA5", 6),
    ]
    fg = [
        P(rect(0, 310, W, 22), "#B8C4D2"),
        P(rect(0, 310, W, 4), "#D2DBE6"),
        P(rect(0, 332, W, 68), "#8C9BAD"),
        P("".join(f"M{x} 332V400" for x in (120, 240, 480, 600)), None, "#7A899C", 3, cap="butt"),
        P(rect(470, 236, 94, 66, 6), "#3B4452"),
        P(rect(508, 300, 18, 12), "#3B4452"),
        P(rect(160, 294, 54, 16, 3), "#23396B"),
        P(circle(187, 302, 5), "#E7B93E"),
        P(rect(606, 268, 16, 24, 6), "#5A3E2B"),
        P(rect(596, 290, 36, 18, 4), "#2F2F38"),
    ]
    return bg, fg


def hotel():
    bg = [
        wall("#F4E9DA", "#E8D6BE"),
        P(rect(240, 0, 240, 400), "#E7D3B8"),
        [P(rect(x, 0, 4, 400), "#D9C2A2") for x in range(252, 480, 24)],
        P(rect(36, 150, 168, 150, 6), "#B8875F"),
        [P(rect(46 + c * 38, 160 + r * 34, 30, 26, 3), "#E9D3B5") for c in range(4) for r in range(4)],
        [P(circle(61 + c * 38, 170 + r * 34, 3.5), "#C9A227") for c in range(4) for r in range(4) if (c + r) % 2 == 0],
        [P(circle(x, 86, 26), WHITE, "#9B7350", 4) for x in (72, 160)],
        P("M72 86V70M72 86L84 92M160 86V68M160 86L150 96", None, "#3A2F28", 3),
        window(500, 52, 190, 156, ("#FF9E6D", "#FFE1B3"), [
            P(rect(500, 176, 190, 40), "#5E86B8"),
            P(circle(640, 120, 18), "#FFF0C2"),
            P("M560 176C566 150 580 140 596 140L590 176ZM586 176C594 144 612 132 630 134L616 176ZM612 176C622 150 640 142 656 146L640 176Z", "#FFF4E6"),
            P("M500 160Q545 116 590 160", None, "#7A5C8F", 4),
        ]),
    ]
    fg = [
        P(rect(0, 310, W, 20), "#F5F0E8"),
        P("M60 316L120 324M300 314L340 326M560 318L640 322", None, "#D9D2C7", 2),
        P(rect(0, 330, W, 70), "#8B5E3C"),
        P("".join(f"M{x} 330V400" for x in range(30, W, 36)), None, "#7A5032", 3, cap="butt"),
        P(ellipse(214, 308, 26, 5), "#B08D2A"),
        P("M192 306A22 20 0 0 1 236 306Z", "#E8C547"),
        P(circle(214, 284, 4), "#B08D2A"),
        P(rect(520, 290, 40, 20, 4), "#3B4452"),
        P(rect(526, 280, 28, 12, 2), WHITE),
        P(poly((606, 310), (630, 310), (626, 270), (610, 270)), "#7FB6D5", alpha=0.7),
        P("M618 270C614 246 620 236 618 226M618 256C628 246 632 238 640 236", None, "#4FA36F", 3),
        P(circle(618, 222, 9), "#FF7AA2"),
        P(circle(641, 233, 7), "#FFD166"),
    ]
    return bg, fg


def shopping():
    shirts = []
    for i, c in enumerate(("#E4572E", "#4ECDC4", "#FFD166", "#7B68EE", "#FF7AA2")):
        x = 44 + i * 38
        shirts += [
            P(f"M{x} 120L{x + 14} 108L{x + 28} 120", None, "#9AA5B1", 2.5),
            P(f"M{x - 4} 128L{x + 8} 120H{x + 20}L{x + 32} 128L{x + 28} 140L{x + 26} 200H{x + 2}L{x} 140Z", c),
        ]
    stacks = []
    for r, y in enumerate((112, 182)):
        for i, c in enumerate(("#3D5A99", "#F4C95D", "#8CC7A1", "#E07A5F")):
            for k in range(3):
                stacks.append(P(rect(504 + i * 48, y - 14 - k * 13, 40, 12, 4), c if k != 1 else _tint(c)))
    bg = [
        wall("#F4F0F9", "#E6E0F1"),
        P("M24 108H236", None, "#9AA5B1", 4),
        P("M30 108V96M230 108V96", None, "#9AA5B1", 4),
        shirts,
        stacks,
        P(rect(496, 112, 208, 6, 2), "#B8A48C"),
        P(rect(496, 182, 208, 6, 2), "#B8A48C"),
        lamp(276, 40, shade="#B39DFF", rim="#8E77F0"), lamp(444, 40, shade="#B39DFF", rim="#8E77F0"),
    ]
    fg = [
        counter(surface="#EADBC8", lip="#F5EBDD", front="#CFB79C", line="#BFA585"),
        P(poly((146, 252), (222, 252), (228, 318), (140, 318)), "#6C5CE7"),
        P("M166 252C166 228 202 228 202 252", None, "#4B3BC9", 5),
        P(rect(170, 272, 28, 20, 4), WHITE, alpha=0.9),
        P(rect(532, 270, 56, 48, 6), "#3B4452"),
        P(rect(540, 278, 40, 18, 3), "#9BE3C4"),
        P(rect(470, 300, 50, 18, 5), "#F4C95D"),
        P(rect(474, 286, 42, 15, 5), "#E07A5F"),
    ]
    return bg, fg


def _tint(c):
    r, g, b = (int(c[i:i + 2], 16) for i in (1, 3, 5))
    r, g, b = (min(255, int(v + (255 - v) * 0.3)) for v in (r, g, b))
    return f"#{r:02X}{g:02X}{b:02X}"


def party():
    bg = [
        wall("#3B2E6E", "#5D4C9E"),
        window(40, 66, 170, 150, ("#141038", "#2C2470"), [
            [P(circle(x, y, 2), WHITE, alpha=0.8) for x, y in ((60, 90), (100, 80), (150, 100), (190, 86), (120, 120))],
            P(circle(176, 96, 14), "#FFF3C4"),
            P("M80 216C82 170 84 150 92 130M92 130C80 124 66 128 58 138M92 130C96 118 108 114 118 118M92 130C104 128 114 136 118 146M92 130C84 118 86 106 92 100", None, "#0D0A26", 5),
            P("M160 216C160 180 162 164 168 150M168 150C158 142 146 146 140 154M168 150C174 140 186 140 192 146M168 150C178 150 186 158 188 166", None, "#0D0A26", 5),
        ], frame="#2A2152"),
        string_lights(),
        [P(f"M{x} {y + r}Q{x + 6} {y + r + 30} {x - 4} {y + r + 70}", None, "#E8E3FF", 1.5) for x, y, r in ((580, 110, 26), (630, 90, 28), (676, 120, 24))],
        P(ellipse(580, 110, 24, 28), "#FF7AA2"),
        P(ellipse(630, 90, 26, 30), "#FFD166"),
        P(ellipse(676, 120, 22, 26), "#4ECDC4"),
        [P(ellipse(x - 8, y - 10, 5, 8), WHITE, alpha=0.5) for x, y in ((580, 110), (630, 90), (676, 120))],
    ]
    fg = [
        P(rect(0, 320, W, 80), "#2C2357"),
        P(rect(0, 320, W, 6), "#4A3D86"),
        P("M136 300H224C222 318 206 326 180 326C154 326 138 318 136 300Z", "#FF8C42"),
        [P(poly((x, 300), (x + 14, 284), (x + 26, 300)), "#F6D365") for x in (146, 166, 186, 200)],
        [P(poly((x, 270), (x + 30, 270), (x + 25, 322), (x + 5, 322)), "#E63946") for x in (520, 566)],
        [P(rect(x, 270, 30, 6, 2), WHITE) for x in (520, 566)],
        P(rect(612, 286, 70, 34, 4), "#F4E3C3"),
        P(poly((612, 286), (682, 286), (672, 276), (622, 276)), "#E8D2A8"),
    ]
    return bg, fg


def weekend():
    bg = [
        wall("#D3ECE7", "#B9DCD6"),
        window(40, 62, 170, 150, ("#F6A97A", "#7D6BC4"), [
            P("M40 212V176L70 160L100 176V212ZM104 212V168H140V150H160V212ZM164 212V182L190 166L210 182V212Z", "#4C3F78"),
            [P(rect(x, y, 8, 10), "#FFD98A") for x, y in ((62, 186), (82, 190), (120, 178), (144, 170), (184, 190))],
        ]),
        P(rect(520, 46, 120, 168, 6), "#2D2A40"),
        P(circle(580, 110, 34), "#F2C14E"),
        P(circle(580, 110, 12), "#2D2A40"),
        [P(circle(580 + dx, 110 + dy, 7), "#2D2A40") for dx, dy in ((0, -22), (19, -11), (19, 11), (0, 22), (-19, 11), (-19, -11))],
        P(rect(540, 164, 80, 8, 4), "#F2C14E", alpha=0.8),
        P(rect(552, 180, 56, 6, 3), WHITE, alpha=0.6),
        P(rect(660, 96, 6, 236), "#5C4A3A"),
        P(poly((640, 60), (688, 60), (700, 100), (628, 100)), "#F7E3B5"),
        P(circle(664, 108, 30), "#FFE9A8", alpha=0.3),
    ]
    stripes = [P(rect(x, 252, 10, 70), "#E63946") for x in range(536, 600, 20)]
    fg = [
        P(rect(0, 322, W, 78), "#B07D56"),
        P(rect(0, 322, W, 6), "#C79368"),
        P(rect(116, 300, 120, 22, 4), "#C5CBD3"),
        P(rect(116, 300, 120, 4, 2), "#DDE2E8"),
        [P(circle(538 + i * 12, 254 - (i % 2) * 6, 10), "#FFF3C4") for i in range(6)],
        P(poly((530, 256), (606, 256), (598, 322), (538, 322)), WHITE),
        G(stripes, clip=poly((530, 256), (606, 256), (598, 322), (538, 322))),
        mug(640, 322, "#8E77F0"),
    ]
    return bg, fg


def doctor():
    chart = [P(rect(582 - (90 - i * 16) / 2, 70 + i * 28, 90 - i * 16, 12 - i * 1.5, 2), "#2F3B4C") for i in range(5)]
    bg = [
        wall("#E6F5F0", "#CDEAE1"),
        window(40, 58, 170, 150, ("#BFE3FA", "#EAF6FF"), [
            skyline(208, 40, 210, "#A9C3D9", seed=4),
            P("M118 208V112H126V94H130V74H134V94H138V112H146V208Z", "#8DAFCB"),
        ]),
        P(rect(520, 48, 124, 172, 6), WHITE, "#C7D6DF", 3),
        chart,
        P(circle(682, 100, 22), "#3FA7A0"),
        P("M682 88V112M670 100H694", None, WHITE, 6),
        P(rect(246, 132, 26, 40, 6), "#E3EAF0"),
        P(rect(252, 172, 14, 6, 2), "#9FB2C4"),
    ]
    fg = [
        P(rect(0, 318, W, 16), "#F7F9FA"),
        P(rect(0, 318, W, 3), WHITE),
        P(rect(0, 334, W, 66), "#D5DEE5"),
        P("M240 334V400M480 334V400", None, "#C2CDD6", 3, cap="butt"),
        P(rect(160, 258, 64, 60, 5), "#B8875F"),
        P(rect(168, 268, 48, 48, 3), WHITE),
        P(rect(180, 252, 24, 12, 3), "#9AA5B1"),
        [P(rect(176, y, 32, 4, 2), "#C7D6DF") for y in (280, 292, 304)],
        P(rect(470, 228, 104, 74, 6), "#5B6575"),
        P(rect(510, 300, 24, 18), "#5B6575"),
        P(rect(612, 284, 24, 34, 4), "#7FB6D5"),
        P("M618 284V262M626 284V258M632 284V266", None, "#3D6FD9", 3),
    ]
    return bg, fg


def coworker():
    notes = [P(rect(x, y, 30, 30, 3), c) for x, y, c in ((58, 168, "#FFE27A"), (98, 160, "#FFB3C7"), (142, 172, "#9DE0D6"), (70, 212, "#B8C9FF"), (118, 208, "#FFE27A"))]
    bg = [
        wall("#F6F8FB", "#E5EAF1"),
        P(rect(0, 26, 236, 92, 6), "#D6DEE8"),
        P("M118 26V118", None, "#C2CCD8", 3),
        P("M104 92V104M132 92V104", None, "#8C9BAD", 4),
        P(rect(40, 150, 170, 110, 6), "#D9B38C"),
        P(rect(40, 150, 170, 110, 6), None, "#B8875F", 5),
        notes,
        [P(circle(x + 15, y + 4, 3), "#E4572E") for x, y in ((58, 168), (98, 160), (142, 172), (70, 212), (118, 208))],
        P(rect(482, 84, 200, 8, 2), "#B8C4D2"),
        mug(498, 84, "#FFD166"), mug(548, 84, "#4ECDC4"), mug(598, 84, "#FF7AA2"),
        P(rect(620, 118, 100, 214, 8), "#E3E8EE"),
        P(rect(620, 118, 100, 214, 8), None, "#C9D2DC", 3),
        P("M620 196H720", None, "#C9D2DC", 3),
        P("M632 140V176M632 210V260", None, "#9AA5B1", 5),
    ]
    fg = [
        P(rect(0, 320, W, 14), WHITE),
        P(rect(0, 334, W, 66), "#9FB3C8"),
        P(rect(0, 330, W, 4), "#C9D2DC"),
        P(rect(108, 226, 96, 94, 10), "#2F2F38"),
        P(rect(120, 240, 72, 22, 4), "#4A4F5C"),
        P(rect(144, 270, 24, 8, 2), "#4A4F5C"),
        P(rect(136, 290, 40, 24, 4), "#5B6375"),
        P(ellipse(560, 318, 46, 10), "#E3E8EE"),
        P("M516 312C520 332 600 332 604 312Z", "#C9D2DC"),
        [P(circle(x, y, 12), c) for x, y, c in ((542, 302, "#E4572E"), (562, 298, "#F2A541"), (582, 304, "#E4572E"), (552, 290, "#8CC7A1"))],
    ]
    return bg, fg


def meeting():
    bars = [P(rect(76 + i * 30, 180 - hgt, 18, hgt, 3), c) for i, (hgt, c) in enumerate(((40, "#9C8CF0"), (64, "#7B68EE"), (52, "#9C8CF0"), (88, "#4ECDC4")))]
    bg = [
        wall("#EEF1F7", "#DCE2EE"),
        P(rect(40, 56, 214, 150, 8), WHITE, "#B8C4D2", 6),
        bars,
        P("M74 184H206", None, "#8C9BAD", 3),
        P("M80 120L112 104L142 112L176 88L204 80", None, "#E4572E", 3),
        P(rect(212, 70, 30, 30, 3), "#FFE27A"),
        P(rect(212, 108, 30, 30, 3), "#FFB3C7"),
        window(502, 52, 190, 170, ("#BFE3FA", "#EAF6FF"), [skyline(222, 502, 692, "#A8CBE6", seed=6)], frame="#C9D2DC", cross=False),
        [P(rect(502, y, 190, 4), "#D9E2EC", alpha=0.8) for y in range(66, 222, 18)],
        plant(470, 300, 0.9),
    ]
    return bg, video_ui()


def interview():
    bg = [
        wall("#F7EFE6", "#EADFD1"),
        P(rect(28, 46, 206, 260, 6), "#9B7350"),
        P(rect(38, 56, 186, 240, 4), "#7A5638"),
        shelf(38, 128, 186, ("#E4572E", "#3D5A99", "#F2C14E", "#4FA36F", "#B39DFF", "#E07A5F", "#3D5A99", "#F2C14E", "#8CC7A1", "#E4572E")),
        shelf(38, 210, 186, ("#3D5A99", "#F2C14E", "#E07A5F", "#4FA36F", "#3D5A99", "#B39DFF")),
        plant(190, 202, 0.7),
        shelf(38, 292, 186, ("#F2C14E", "#E4572E", "#8CC7A1", "#3D5A99", "#E07A5F", "#B39DFF", "#F2C14E", "#4FA36F")),
        P(rect(530, 70, 120, 90, 4), "#E8DCC9", "#B8875F", 6),
        P(circle(590, 104, 16), "#3D5A99"),
        P(rect(560, 128, 60, 6, 3), "#B8875F"),
        P(rect(570, 140, 40, 5, 2), "#B8875F", alpha=0.7),
        P(rect(664, 120, 6, 212), "#3A2F28"),
        P(poly((640, 90), (694, 90), (684, 124), (650, 124)), "#F3E0B5"),
        P(circle(667, 132, 30), "#FFE9A8", alpha=0.3),
    ]
    return bg, video_ui()


def negotiation():
    bars = [P(rect(78 + i * 34, 176 - hgt, 20, hgt, 3), "#4ECDC4" if i == 3 else "#9C8CF0") for i, hgt in enumerate((42, 54, 66, 86))]
    bg = [
        wall("#E9EEF5", "#D6DEEA"),
        P(rect(36, 50, 228, 150, 10), "#2B3042"),
        P(rect(44, 58, 212, 134, 6), "#3A4157"),
        bars,
        P(circle(222, 92, 20), "#F2C14E"),
        P("M228 84C222 80 214 82 214 88C214 96 230 92 230 100C230 106 220 108 214 104M222 78V108", None, "#2B3042", 3),
        P(rect(140, 200, 20, 20), "#2B3042"),
        P(rect(110, 218, 80, 6, 3), "#2B3042"),
        window(474, 40, 222, 196, ("#9ED0F0", "#E3F3FC"), [
            skyline(236, 474, 696, "#9DB8CF", seed=8),
            skyline(236, 490, 690, "#B8CDE0", seed=1),
        ], frame="#C9D2DC"),
        plant(448, 306, 1.0, pot="#3A4157"),
    ]
    return bg, video_ui()


def complaint():
    tape = "#E8C18A"
    bg = [
        wall("#F3E8DD", "#E6D5C4"),
        window(40, 60, 160, 140, ("#BFE3FA", "#EAF6FF"), [cloud(90, 100, 0.8), P(rect(40, 172, 160, 30), "#9CC48B")]),
        P(rect(540, 210, 150, 110, 4), "#C9A06B"),
        P(rect(560, 140, 110, 72, 4), "#D4AD78"),
        P("M540 240H690M560 166H670", None, tape, 8, cap="butt"),
        P(poly((600, 268), (612, 252), (624, 268)), "#8A6A45"),
        P("M612 268V290", None, "#8A6A45", 4),
        P(rect(470, 96, 46, 60, 4), "#9B7350"),
        P(rect(476, 102, 34, 48, 2), "#E9F1F7"),
        P(circle(493, 120, 8), "#FFB3C7"),
    ]
    fg = [
        P(rect(0, 318, W, 18), "#D9B48A"),
        P(rect(0, 318, W, 4), "#E8C9A3"),
        P(rect(0, 336, W, 64), "#B88E62"),
        P("M188 324L214 330L236 323L262 331L286 324", None, "#7A5230", 3.5),
        P(rect(560, 288, 80, 30, 4), "#3B4452"),
        P(rect(566, 294, 68, 6, 2), "#5B6375"),
        P(rect(92, 292, 52, 26, 3), WHITE),
        [P(rect(98, y, 40, 3, 1), "#C7D6DF") for y in (298, 305, 312)],
    ]
    return bg, fg


def debate():
    bg = [
        wall("#2F3B52", "#3F4D69"),
        window(40, 60, 170, 150, ("#1B2340", "#33406A"), [
            skyline(210, 40, 210, "#141A33", seed=3),
            [P(rect(x, y, 5, 6), "#FFD98A") for x, y in ((58, 176), (84, 168), (104, 184), (150, 172), (176, 186), (196, 178))],
        ], frame="#55627F"),
        picture(530, 70, 130, 90, frame="#C79A5F", sky=("#F6A97A", "#FFE1B3"), hills=("#7D6BC4", "#5D4C9E")),
        lamp(276, 50), lamp(444, 50),
    ]
    fg = [
        P(rect(0, 320, W, 80), "#8A5A3B"),
        P(rect(0, 320, W, 5), "#A56E4A"),
        P(ellipse(150, 322, 60, 14), WHITE),
        P(ellipse(150, 318, 34, 9), "#E07A5F"),
        P(ellipse(140, 314, 12, 5), "#8CC7A1"),
        P(ellipse(570, 322, 60, 14), WHITE),
        P(ellipse(570, 318, 34, 9), "#F2C14E"),
        P("M244 318H268M256 318V292", None, "#DDE6EE", 3),
        P("M242 262H270C270 282 264 292 256 292C248 292 242 282 242 262Z", WHITE, "#DDE6EE", 2, alpha=0.5, stroke_alpha=1),
        P("M244 272H268C266 284 262 290 256 290C250 290 246 284 244 272Z", "#9B1D3A"),
        G(
            P(rect(452, 300, 70, 46, 3), "#F3F1EC"),
            P(rect(458, 306, 40, 6, 2), "#2F3B4C"),
            P(rect(458, 316, 26, 18, 2), "#9DB8CF"),
            P(rect(488, 316, 28, 3, 1), "#9AA5B1"),
            P(rect(488, 323, 28, 3, 1), "#9AA5B1"),
            rot=-8, px=487, py=323,
        ),
    ]
    return bg, fg


def qanda():
    heads = []
    for row, (y, c) in enumerate(((258, "#1A2148"), (294, "#151B3D"))):
        for i in range(9):
            x = 30 + i * 80 + row * 40
            if 260 < x < 460:
                continue
            heads += [P(circle(x, y, 18), c), P(f"M{x - 30} {y + 50}C{x - 28} {y + 24} {x - 14} {y + 16} {x} {y + 16}C{x + 14} {y + 16} {x + 28} {y + 24} {x + 30} {y + 50}Z", c)]
    bg = [
        wall("#1B2347", "#2B3768"),
        P(poly((120, 0), (180, 0), (300, 400), (40, 400)), "#FFF6D6", alpha=0.08),
        P(poly((540, 0), (600, 0), (680, 400), (420, 400)), "#FFF6D6", alpha=0.08),
        P(rect(0, 230, W, 170), "#222B57"),
        heads,
        [P(circle(x, 24, 8), "#FFF6D6", alpha=0.9) for x in (150, 570)],
        P(rect(630, 50, 56, 24, 4), "#2E9C5C"),
        P(rect(640, 58, 22, 8, 2), WHITE, alpha=0.9),
        P(poly((668, 56), (678, 62), (668, 68)), WHITE),
        [P(rect(x, 120, 6, 50, 3), "#3A4A8C") for x in (60, 660)],
    ]
    seats = []
    for i in range(8):
        x = i * 96 - 8
        seats += [P(rect(x, 330, 88, 70, 18), "#3B4A8C"), P(rect(x + 6, 336, 76, 10, 5), "#4C5DA6")]
    fg = seats
    return bg, fg


def free():
    land = "#8CC7A1"
    bg = [
        wall("#FFF3E4", "#F6E0C5"),
        string_lights(28, 30, ("#FFE9A8", "#FFD166", "#FFE9A8", "#FFC46B")),
        P(rect(34, 70, 220, 132, 6), "#B8875F"),
        P(rect(40, 76, 208, 120, 3), "#CFE8F7"),
        P("M58 100C70 88 96 90 104 100C110 112 98 128 84 126C76 140 64 132 62 120C52 116 52 106 58 100Z", land),
        P("M90 136C100 134 106 146 104 160C102 176 92 184 88 172C84 160 82 144 90 136Z", land),
        P("M132 96C146 86 172 90 190 96C206 98 222 108 220 122C212 132 196 128 186 136C176 130 164 132 156 124C144 120 134 110 132 96Z", land),
        P("M140 132C152 130 160 142 156 158C152 170 142 172 138 160C134 150 134 138 140 132Z", land),
        P("M206 152C216 150 226 158 222 168C214 174 204 168 206 152Z", land),
        P(circle(98, 112, 5), "#E4572E"),
        G(P(rect(500, 70, 72, 84, 3), WHITE), P(rect(506, 76, 60, 56), "#7FC8F8"), P(circle(536, 112, 12), "#FFD166"), rot=-8, px=536, py=112),
        G(P(rect(588, 60, 72, 84, 3), WHITE), P(rect(594, 66, 60, 56), "#8CC7A1"), P(poly((594, 122), (620, 90), (654, 122)), "#4FA36F"), rot=6, px=624, py=102),
        G(P(rect(548, 168, 72, 84, 3), WHITE), P(rect(554, 174, 60, 56), "#F6A97A"), P(circle(584, 206, 14), "#FFF3C4"), rot=-3, px=584, py=210),
        plant(676, 300, 1.1),
    ]
    fg = [
        P(rect(0, 320, W, 80), "#A9744F"),
        P(rect(0, 320, W, 5), "#C08A62"),
        mug(150, 320, WHITE, "#E07A5F"),
        P("M156 282C150 272 160 266 156 256M168 282C162 272 172 266 168 256", None, WHITE, 3, alpha=0.9),
        mug(540, 320, WHITE, "#4ECDC4"),
        G(P(rect(212, 296, 70, 24, 3), "#3D5A99"), P(rect(212, 296, 70, 5, 2), "#F3EDE2"), rot=-4, px=247, py=308),
        P(rect(600, 290, 56, 30, 6), "#2F2F38"),
        P(circle(628, 305, 10), "#5B6375"),
        P(circle(628, 305, 5), "#9BB7D4"),
        P(rect(606, 284, 14, 8, 2), "#2F2F38"),
    ]
    return bg, fg


# シナリオ id → (場面を作る関数, 相手の人物)
SCENES = {
    "cafe": (cafe, "jamie"),
    "directions": (directions, "pat"),
    "restaurant": (restaurant, "marco"),
    "immigration": (immigration, "reed"),
    "hotel": (hotel, "olivia"),
    "shopping": (shopping, "sam"),
    "party": (party, "chris"),
    "weekend": (weekend, "taylor"),
    "doctor": (doctor, "drlee"),
    "coworker": (coworker, "jordan"),
    "meeting": (meeting, "morgan"),
    "interview": (interview, "carter"),
    "negotiation": (negotiation, "dana"),
    "complaint": (complaint, "casey"),
    "debate": (debate, "riley"),
    "qanda": (qanda, "patel"),
    "free": (free, "alex"),
}


def confetti():
    """振り返り画面で場面に重ねる紙吹雪 (顔のあたりは空ける)"""
    return confetti_bits((0, 0, 260, 300), 16, 3) + confetti_bits((460, 0, 720, 300), 16, 11) + confetti_bits((260, 0, 460, 90), 6, 5)


def league_bg():
    """リーグ画面の背景 (720x320)。表彰台の上に 3 人を重ね、前に台の前面を描く"""
    rays = [P(poly((360, 260), (360 + 900 * dx, -400), (360 + 900 * (dx + 0.12), -400)), WHITE, alpha=0.08) for dx in (-1.2, -0.75, -0.3, 0.15, 0.6, 1.05)]
    return [
        P(rect(0, 0, W, 320), vgrad(0, 320, "#7B68EE", "#B3A6FF")),
        rays,
        confetti_bits((0, 0, 720, 200), 30, 19),
        [P(star4(x, y, r), "#FFE27A") for x, y, r in ((90, 70, 14), (640, 60, 16), (560, 150, 9), (150, 170, 10))],
    ]


def league_fg():
    return [
        P(rect(270, 214, 180, 106, 10), "#FFD166"),
        P(rect(270, 214, 180, 14, 7), "#FFE29A"),
        P(rect(110, 246, 160, 74, 10), "#E3E8EE"),
        P(rect(110, 246, 160, 12, 6), "#F5F7FA"),
        P(rect(450, 268, 160, 52, 10), "#F0B37E"),
        P(rect(450, 268, 160, 12, 6), "#F7CDA6"),
        P(circle(360, 266, 18), "#F2A541"),
        P(poly((350, 246), (360, 258), (370, 246)), "#E4572E"),
        P(circle(190, 286, 15), "#C9D3E6"),
        P(circle(530, 296, 13), "#C98A4B"),
    ]
