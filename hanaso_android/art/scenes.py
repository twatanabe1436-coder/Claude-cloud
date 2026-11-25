"""会話の場面 (29 か所) の背景と前景。

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


# ---- v0.6.0 で足した B1・B2 の場面 ----

def _plane(cx, cy, s=1.0, body=WHITE, accent="#3A6EA5"):
    return G(
        P(ellipse(0, 0, 46, 8), body),
        P(poly((-6, -2), (12, -2), (-8, -26), (-18, -26)), accent),
        P(poly((-4, 4), (14, 4), (-6, 22), (-16, 22)), body),
        P(poly((-40, -4), (-30, -4), (-40, -20), (-46, -20)), accent),
        P("M-26 -2H30", None, "#9DB8CF", 2),
        tx=cx, ty=cy, s=s, rot=-6,
    )


def airport_hall(storm=False):
    sky = ("#4E5A72", "#7D8BA3") if storm else ("#9ED0F0", "#E3F3FC")
    view = [P(rect(36, 172, 214, 40), "#8C9BAD"), P("M36 190H250", None, "#F2C14E", 3)]
    if storm:
        view += [P(circle(x, y, 3), WHITE, alpha=0.9) for x, y in ((56, 70), (90, 110), (120, 80), (150, 130), (186, 66), (214, 104), (236, 140), (70, 150), (104, 176), (200, 170))]
        view.append(P(rect(36, 196, 214, 16), WHITE, alpha=0.85))
    else:
        view += [cloud(90, 80, 0.7), _plane(170, 110, 0.9)]
    rows = []
    for i in range(5):
        y = 92 + i * 20
        bad = storm and i in (0, 2, 3)
        rows += [
            P(rect(484, y, 26, 8, 2), "#FFD166"),
            P(rect(518, y, 84, 8, 2), WHITE, alpha=0.85),
            P(rect(612, y, 60, 8, 2), "#E5484D" if bad else "#46C37B"),
        ]
    bg = [
        wall("#EEF2F7", "#D9E1EC"),
        window(42, 56, 202, 150, sky, view, frame="#C9D2DC", cross=False),
        [P(rect(x, 56, 4, 150), "#C9D2DC") for x in (108, 176)],
        P(rect(470, 52, 216, 152, 10), "#1E2533"),
        P(rect(470, 52, 216, 28, 10), "#2E3A52"),
        P(rect(484, 62, 60, 8, 3), WHITE, alpha=0.9),
        G(P(ellipse(0, 0, 13, 3), WHITE), P(poly((-2, -1), (5, -1), (-4, -9)), WHITE), P(poly((-2, 1), (5, 1), (-4, 9)), WHITE), tx=660, ty=66),
        rows,
        P(rect(298, 50, 124, 36, 10), "#D64545" if storm else "#1F3B73"),
        _plane(360, 68, 0.38, WHITE, WHITE),
        P(rect(476, 230, 8, 102), "#9AA5B1"),
        P(rect(640, 230, 8, 102), "#9AA5B1"),
        P("M484 244H640", None, "#3A6EA5", 6),
    ]
    fg = [
        counter(surface="#E3E8EE", lip="#F5F7FA", front="#2E4A7D", line="#26406C"),
        P(rect(0, 334, W, 6), "#D64545" if storm else "#F2C14E"),
        P(rect(96, 240, 84, 104, 14), "#E07A5F"),
        P(rect(96, 240, 84, 104, 14), None, "#C4624A", 3),
        P("M124 240V226H152V240", None, "#4A4F5C", 5),
        P("M112 262V326M164 262V326", None, "#C4624A", 4),
        P(circle(112, 348, 7), "#2F2F38"),
        P(circle(164, 348, 7), "#2F2F38"),
        P(rect(196, 290, 46, 26, 4), "#3B4452"),
        P(rect(202, 296, 34, 12, 2), "#9BE3C4"),
        P(rect(530, 262, 86, 56, 6), "#3B4452"),
        P(rect(538, 270, 70, 30, 3), "#7FB6D5"),
        P(rect(546, 304, 54, 6, 2), WHITE),
    ]
    return bg, fg


def airport():
    return airport_hall(False)


def flight():
    return airport_hall(True)


def lostfound():
    tiles = P("".join(f"M0 {y}H{W}" for y in range(20, 230, 22)) + "".join(f"M{x} 0V230" for x in range(36, W, 44)), None, "#C9D6D0", 1.5, cap="butt")
    items = [
        P(rect(500, 74, 176, 6, 2), "#8C9BAD"),
        P(rect(500, 154, 176, 6, 2), "#8C9BAD"),
        P(rect(500, 234, 176, 6, 2), "#8C9BAD"),
        P(rect(510, 34, 44, 40, 10), "#E07A5F"),
        P(rect(566, 44, 34, 30, 6), "#F2C14E"),
        P(rect(612, 30, 52, 44, 12), "#7B68EE"),
        P("M520 154V110M540 154V104M560 154V112", None, "#3D5A99", 4),
        P("M520 110A8 8 0 0 0 504 110M540 104A8 8 0 0 0 524 104M560 112A8 8 0 0 0 544 112", None, "#3D5A99", 4),
        P(rect(586, 112, 40, 42, 8), "#4FA36F"),
        P(ellipse(650, 146, 22, 8), "#5C4A3A"),
        P(ellipse(650, 136, 14, 12), "#5C4A3A"),
        P(rect(512, 196, 50, 38, 8), "#9AA5B1"),
        P(rect(576, 186, 60, 48, 10), "#E4572E"),
        P(rect(646, 202, 30, 32, 6), "#3D5A99"),
    ]
    bg = [
        wall("#F2F6F4", "#E2EAE6"),
        tiles,
        P(rect(0, 230, W, 16), "#2E9C5C"),
        P(rect(0, 246, W, 6), "#1E7A45"),
        P(rect(36, 60, 200, 58, 8), "#1E2230"),
        P(circle(68, 89, 18), "#2E9C5C"),
        P("M74 80H62V98H74V89H68", None, WHITE, 4),
        P(rect(98, 78, 110, 8, 3), WHITE, alpha=0.9),
        P(rect(98, 94, 74, 6, 3), WHITE, alpha=0.6),
        P(rect(298, 48, 124, 38, 10), "#F2C14E"),
        P(circle(346, 66, 9), None, "#2F2F38", 3.5),
        P("M352 72L362 82", None, "#2F2F38", 4),
        P(rect(368, 62, 40, 6, 3), "#2F2F38", alpha=0.7),
        P(rect(488, 20, 200, 300, 8), "#D5DEE5"),
        items,
    ]
    fg = [
        counter(surface="#C9D2DC", lip="#E3E8EE", front="#5B6B7A", line="#4C5A68"),
        P(rect(120, 286, 66, 32, 3), WHITE),
        [P(rect(128, y, 48, 3, 1), "#C7D6DF") for y in (293, 300, 307)],
        P(rect(144, 280, 18, 8, 2), "#9AA5B1"),
        P(rect(514, 246, 84, 76, 16), "#2F2F38"),
        P(rect(526, 278, 60, 38, 10), "#3B3F4A"),
        P("M536 250C536 230 576 230 576 250", None, "#2F2F38", 6),
        P(rect(548, 288, 16, 4, 2), "#7FB6D5"),
        P(ellipse(640, 316, 16, 4), "#B8C4D2"),
        P("M628 314A12 12 0 0 1 652 314Z", "#E3B341"),
    ]
    return bg, fg


def reservation():
    bottles = []
    for y in (100, 160, 220):
        for i in range(5):
            x = 512 + i * 34
            c = "#5B2333" if (i + y // 60) % 2 else "#2F5D3A"
            bottles += [P(circle(x + 10, y, 11), c), P(circle(x + 10, y, 4), "#C9A227")]
    bg = [
        wall("#F3E2CF", "#E7CDB0"),
        P(rect(0, 236, W, 164), "#8B5E3C"),
        P(rect(0, 236, W, 6), "#A36F49"),
        window(40, 56, 176, 150, ("#BFE3FA", "#EAF6FF"), [
            skyline(206, 40, 216, "#C99A7A", seed=9),
            P(rect(40, 186, 176, 24), "#8C9BAD"),
            P(circle(186, 86, 14), "#FFE7A8"),
        ], frame="#F7EBDD"),
        P(rect(496, 52, 186, 196, 8), "#6B4226"),
        [P(rect(496, y, 186, 6), "#4E2F1A") for y in (112, 172, 232)],
        bottles,
        lamp(276, 46, shade="#7A4B32", rim="#5E3825"), lamp(444, 46, shade="#7A4B32", rim="#5E3825"),
    ]
    fg = [
        counter(surface="#5E3825", lip="#7A4B32", front="#4A2C1C", line="#3B2316"),
        P(rect(110, 290, 66, 28, 3), "#FFF6E8"),
        P(rect(178, 290, 66, 28, 3), "#FFF6E8"),
        P("M177 290V318", None, "#D9C2A2", 2),
        [P(rect(118, y, 50, 2.5, 1), "#C9B49A") for y in (298, 304, 310)],
        [P(rect(186, y, 50, 2.5, 1), "#C9B49A") for y in (298, 304)],
        P(rect(196, 306, 30, 3, 1.5), "#D64545"),
        P("M232 316L254 292", None, "#2F2F38", 3),
        P(poly((592, 318), (618, 318), (614, 272), (596, 272)), "#7FB6D5", alpha=0.7),
        P("M605 272C600 252 606 240 604 230M605 262C614 250 620 244 628 242M605 258C596 248 590 244 584 244", None, "#4FA36F", 3),
        P(circle(604, 226, 9), "#FF7AA2"),
        P(circle(629, 240, 7), "#FFD166"),
        P(circle(583, 241, 7), "#FF7AA2"),
    ]
    return bg, fg


def hallway(kind):
    moving = kind == "moving"
    wood, line = ("#8B5E3C", "#6B4226") if moving else ("#3D5A99", "#30497D")
    door = [
        P(rect(34, 66, 152, 270, 6), "#D9C2A2"),
        P(rect(46, 76, 128, 260, 4), wood),
        P(rect(60, 94, 100, 90, 4), None, line, 3),
        P(rect(60, 200, 100, 110, 4), None, line, 3),
        P(circle(158, 214, 7), "#E3B341"),
        P(rect(92, 52, 36, 18, 4), "#E3B341"),
        P(circle(110, 120, 4), "#2F2F38"),
    ]
    extra = []
    if moving:
        extra += [
            P(circle(250, 106, 26), "#FFE9A8", alpha=0.45),
            P(rect(242, 96, 16, 22, 6), "#F7E3B5"),
            plant(470, 318, 0.9),
        ]
    else:
        extra += [
            P(rect(206, 90, 54, 76, 3), "#2F2F38"),
            P(circle(233, 118, 14), "#E4572E"),
            P(rect(214, 144, 38, 6, 2), "#FFD166"),
            P("M572 140V108L596 102V134", None, "#7B68EE", 3),
            P(ellipse(568, 141, 6, 4.5, -20), "#7B68EE"),
            P(ellipse(592, 135, 6, 4.5, -20), "#7B68EE"),
            P("M462 196V170", None, "#E4572E", 3),
            P(ellipse(458, 197, 6, 4.5, -20), "#E4572E"),
        ]
    bg = [
        wall("#F1E6D6", "#E4D3BC"),
        P(rect(0, 318, W, 14), "#C9A27A"),
        door,
        window(516, 60, 160, 170, ("#BFE3FA", "#EAF6FF") if moving else ("#FFC48A", "#FFE7C2"), [
            P(circle(560, 200, 40), "#7ACB78"),
            P(circle(624, 210, 34), "#6BBF6B"),
            P(rect(516, 214, 160, 16), "#8CC7A1"),
        ], frame="#F7EBDD"),
        extra,
    ]
    fg = [
        P(rect(0, 330, W, 70), "#B8875F"),
        P("".join(f"M{x} 330V400" for x in range(40, W, 80)), None, "#A3764F", 3, cap="butt"),
        P(rect(0, 330, W, 4), "#9C6E48"),
    ]
    if moving:
        tape = "#E8C18A"
        fg += [
            P(rect(30, 260, 92, 80, 4), "#C9A06B"),
            P(rect(130, 276, 80, 64, 4), "#D4AD78"),
            P(rect(46, 206, 70, 56, 4), "#D4AD78"),
            P("M30 290H122M130 300H210M46 228H116", None, tape, 7, cap="butt"),
            P(rect(574, 316, 86, 20, 6), "#8C5A3C"),
            P(rect(584, 300, 30, 16, 6), "#E07A5F"),
            P(rect(620, 304, 30, 12, 6), "#3D5A99"),
        ]
    else:
        fg += [
            G(
                P(ellipse(0, 40, 34, 40), "#C0392B"),
                P(ellipse(0, -10, 26, 30), "#C0392B"),
                P(circle(0, 18, 10), "#2F2F38"),
                P(rect(-5, -110, 10, 90, 2), "#5C3A24"),
                P(rect(-9, -128, 18, 22, 4), "#2F2F38"),
                P(rect(-14, 58, 28, 6, 2), "#2F2F38"),
                tx=590, ty=270, rot=12,
            ),
            P(rect(104, 300, 110, 30, 6), "#2F2F38"),
            P(rect(112, 306, 94, 14, 3), "#4A4F5C"),
            P(circle(130, 313, 5), "#9AA5B1"),
            P(circle(150, 313, 5), "#9AA5B1"),
        ]
    return bg, fg


def neighbor():
    return hallway("moving")


def noise():
    return hallway("music")


def homestay():
    stripes = [P(rect(x, 0, 14, 318), "#EAD7BC", alpha=0.6) for x in range(10, W, 44)]
    bg = [
        wall("#F6EBD9", "#EEDDC4"),
        stripes,
        window(40, 62, 176, 150, ("#F6A97A", "#8E7CC3"), [
            P("M40 212C80 170 120 180 150 196C176 176 200 170 216 176V212Z", "#4C6B4F"),
            P("M40 212C70 196 110 190 140 202C170 196 196 200 216 206V212Z", "#3E5A41"),
            P(circle(170, 120, 14), "#FFE7A8"),
        ], frame=WHITE),
        lamp(360, 30, shade="#E07A5F", rim="#C4624A"),
        picture(510, 70, 76, 56, frame="#9B7350", sky=("#FFD9A8", "#FFF1DA")),
        picture(604, 92, 64, 82, frame="#9B7350", sky=("#BFE3FA", "#EAF6FF"), hills=("#F2C14E", "#E0A030")),
        picture(520, 150, 56, 46, frame="#9B7350", sky=("#F7B6C6", "#FFE4EC"), hills=("#B39DFF", "#8E77F0")),
        P(rect(470, 226, 220, 100, 6), "#9B7350"),
        P(rect(480, 236, 200, 36, 3), "#B8875F"),
        P(rect(480, 280, 200, 36, 3), "#B8875F"),
        P(circle(580, 254, 4), "#E3B341"),
        P(circle(580, 298, 4), "#E3B341"),
    ]
    fg = [
        P(rect(0, 316, W, 84), "#FFF6E8"),
        P(rect(0, 316, W, 4), "#F3E3CF"),
        P(rect(0, 346, W, 24), "#8CC7A1", alpha=0.6),
        P(ellipse(160, 318, 76, 16), WHITE, "#E5D5C3", 2),
        P(ellipse(112, 308, 16, 9, 30), "#B8682F"),
        P(ellipse(208, 308, 16, 9, -30), "#B8682F"),
        P(ellipse(160, 300, 52, 26), "#C8783E"),
        P(ellipse(148, 290, 26, 10, -12), "#E09A5A", alpha=0.8),
        P(circle(97, 316, 5), "#FFF6E8"),
        P(circle(223, 316, 5), "#FFF6E8"),
        [P(circle(x, y, 7), c) for x, y, c in ((128, 322, "#4FA36F"), (192, 322, "#E4572E"))],
        P("M520 300H612C610 322 590 332 566 332C542 332 522 322 520 300Z", "#F3F1EC", "#D9D2C7", 2),
        [P(circle(x, y, 10), c) for x, y, c in ((544, 298, "#5BB57C"), (566, 292, "#4FA36F"), (588, 298, "#8CC7A1"), (556, 302, "#E4572E"), (580, 304, "#F2C14E"))],
        P(poly((640, 268), (668, 268), (664, 322), (644, 322)), "#BFE3FA", "#9DB8CF", 2, alpha=0.6, stroke_alpha=1),
        P(poly((642, 290), (666, 290), (664, 322), (644, 322)), "#7FC8F8", alpha=0.6),
    ]
    return bg, fg


def favor():
    bars = [P(rect(124 + i * 24, 290 - hgt, 16, hgt, 2), c) for i, (hgt, c) in enumerate(((22, "#4ECDC4"), (34, "#7B68EE"), (18, "#4ECDC4"), (40, "#E4572E"), (28, "#7B68EE")))]
    bg = [
        wall("#F2F5F9", "#E1E7EF"),
        window(36, 54, 226, 150, ("#9ED0F0", "#E3F3FC"), [
            skyline(204, 36, 262, "#B8CDE0", seed=4),
            P(rect(80, 120, 18, 84), "#8DAFCB"),
            P(rect(110, 116, 18, 88), "#8DAFCB"),
            P(rect(140, 120, 18, 84), "#8DAFCB"),
            P(rect(70, 108, 98, 10, 4), "#7A9DBC"),
            P(circle(214, 150, 24), "#8CC7A1"),
            P(rect(210, 160, 8, 44), "#6FB38A"),
        ], frame="#C9D2DC", cross=False),
        P(circle(586, 104, 30), WHITE, "#8C9BAD", 4),
        P("M586 104V84M586 104L602 112", None, "#2F3B4C", 3.5),
        P("M660 40V92", None, "#8C9BAD", 2),
        P(ellipse(648, 126, 8, 18, 20), "#4FA36F"),
        P(ellipse(672, 128, 8, 20, -20), "#3E8F5E"),
        P(poly((640, 92), (680, 92), (674, 118), (646, 118)), "#E07A5F"),
        P(rect(470, 226, 250, 94, 6), "#C9D2DC"),
        P(rect(470, 226, 250, 8, 4), "#B8C4D2"),
        P(rect(504, 196, 60, 34, 4), "#3B4452"),
        P(rect(620, 202, 50, 28, 4), "#3B4452"),
    ]
    fg = [
        counter(surface="#EDE6DA", lip="#F7F2EA", front="#C9B79C", line="#B8A585", planks=False),
        P(rect(100, 214, 150, 96, 8), "#2F2F38"),
        P(rect(108, 222, 134, 78, 4), WHITE),
        P(rect(108, 222, 134, 12, 2), "#2E9C5C"),
        [P(rect(112, y, 126, 1.5), "#E3E8EE") for y in range(244, 300, 10)],
        bars,
        P(rect(164, 308, 22, 10), "#2F2F38"),
        P(rect(138, 316, 74, 4, 2), "#2F2F38"),
        mug(520, 318, WHITE, "#E07A5F"),
        P("M526 280C522 272 530 266 526 258M538 280C534 272 542 266 538 258", None, "#C9D2DC", 3),
        P(poly((610, 318), (640, 318), (636, 296), (614, 296)), "#E3B341"),
        P(rect(616, 260, 18, 38, 9), "#5BB57C"),
        P(rect(604, 272, 12, 16, 6), "#5BB57C"),
        P(rect(634, 266, 12, 18, 6), "#5BB57C"),
    ]
    return bg, fg


def deadline():
    days = []
    for r in range(4):
        for c in range(5):
            x, y = 58 + c * 26, 112 + r * 22
            days.append(P(rect(x, y, 20, 16, 2), "#EEF2F6"))
    bg = [
        wall("#EEE7DD", "#DED2C2"),
        P(rect(44, 70, 152, 144, 6), WHITE, "#C9B79C", 3),
        P(rect(44, 70, 152, 30, 6), "#C0392B"),
        P("M80 64V80M160 64V80", None, "#5C4A3A", 5),
        days,
        P(circle(68 + 4 * 26, 120 + 22, 15), None, "#E5484D", 3.5),
        P(rect(210, 230, 52, 70, 3), "#9B7350"),
        P(rect(216, 236, 40, 58, 2), "#FFF6E8"),
        P(circle(236, 256, 8), "#E3B341"),
        window(486, 50, 200, 176, ("#9AA8B8", "#C9D3DE"), [
            skyline(226, 486, 686, "#7D8B9C", seed=7),
            P(rect(612, 92, 26, 134), "#6B7888"),
            P(poly((608, 94), (625, 60), (642, 94)), "#6B7888"),
            P(circle(625, 114, 9), "#E3E8EE"),
            P("M625 114V108M625 114L630 116", None, "#6B7888", 2),
            [P(f"M{x} {y}l-6 14", None, WHITE, 1.5, alpha=0.7) for x, y in ((510, 70), (540, 110), (570, 80), (660, 100), (680, 150), (530, 160), (590, 60))],
        ], frame="#F7EBDD"),
    ]
    fg = [
        counter(surface="#6B4226", lip="#8B5E3C", front="#5A361F", line="#4A2C1A"),
        P(poly((112, 236), (232, 236), (240, 314), (104, 314)), "#9AA5B1"),
        P(poly((118, 242), (226, 242), (232, 308), (112, 308)), "#B8C4D2"),
        P(circle(172, 274, 7), WHITE, alpha=0.8),
        P(rect(92, 312, 160, 8, 3), "#8C9BAD"),
        [P(rect(520 - i * 2, 304 - i * 6, 90, 6, 1), WHITE if i % 2 else "#F4F1EA", "#D9D2C7", 1) for i in range(5)],
        P(rect(626, 268, 30, 50, 4), "#2F3B4C"),
        P("M634 268L630 244M642 268L646 240M650 268L656 248", None, "#E4572E", 3),
    ]
    return bg, fg


def feedback():
    bullets = []
    for i in range(6):
        y = 98 + i * 16
        bullets += [P(circle(60, y + 3, 3), "#7B68EE"), P(rect(70, y, 150 - (i * 23) % 60, 6, 3), "#9AA5B1")]
    bg = [
        wall("#EEF1F7", "#DCE2EE"),
        P(rect(36, 50, 214, 160, 6), WHITE, "#B8C4D2", 4),
        P(rect(36, 50, 214, 34, 6), "#7B68EE"),
        P(rect(50, 62, 110, 10, 4), WHITE, alpha=0.9),
        bullets,
        P(rect(226, 182, 16, 20, 3), "#E5484D", alpha=0.9),
        P(rect(482, 44, 220, 280, 6), "#D6E9F5", alpha=0.7),
        P(rect(482, 44, 220, 280, 6), None, "#B8C4D2", 4),
        P(rect(482, 150, 220, 34), WHITE, alpha=0.6),
        P("M592 44V324", None, "#B8C4D2", 4),
        plant(536, 300, 0.8),
        plant(650, 300, 0.9, pot="#7FB6D5"),
        P(rect(500, 160, 24, 24, 3), "#FFE27A"),
        P(rect(530, 164, 24, 24, 3), "#FFB3C7"),
    ]
    fg = [
        counter(surface="#F4F6F9", lip=WHITE, front="#B8C4D2", line="#A6B3C2", planks=False),
        P(poly((112, 244), (222, 244), (228, 314), (106, 314)), "#9AA5B1"),
        P(rect(96, 312, 142, 7, 3), "#8C9BAD"),
        P(circle(167, 278, 6), WHITE, alpha=0.8),
        mug(250, 318, WHITE, "#7B68EE"),
        P(rect(520, 290, 96, 28, 3), "#3D5A99"),
        P(rect(526, 294, 84, 20, 2), WHITE),
        [P(rect(532, y, 60, 2.5, 1), "#C7D6DF") for y in (299, 305)],
        P("M596 314L640 286", None, "#2F2F38", 3),
        mug(650, 318, "#FFD166"),
    ]
    return bg, fg


def fraud():
    bg = [
        wall("#E8F0F7", "#D3E1EE"),
        P(rect(36, 60, 210, 140, 10), "#1E2A44"),
        P(rect(44, 68, 194, 124, 6), "#25345A"),
        P("M141 84L170 96V122C170 140 157 152 141 160C125 152 112 140 112 122V96Z", "#46C37B"),
        P("M128 120L138 130L156 108", None, WHITE, 5),
        [P(rect(60, y, 40, 6, 3), "#7FB6D5", alpha=0.6) for y in (170, 180)],
        [P(rect(182, y, 40, 6, 3), "#7FB6D5", alpha=0.6) for y in (170, 180)],
        P(poly((326, 70), (360, 52), (394, 70)), "#3D5A99"),
        [P(rect(x, 74, 8, 22), "#3D5A99") for x in (332, 356, 380)],
        P(rect(322, 98, 76, 6, 2), "#3D5A99"),
        window(484, 54, 200, 164, ("#9ED0F0", "#E3F3FC"), [
            P(rect(484, 176, 200, 50), "#5E86B8"),
            P("M530 178C536 150 552 140 568 140L562 178ZM556 178C566 144 586 132 604 134L590 178ZM584 178C596 150 616 142 632 146L616 178Z", WHITE),
            P(rect(520, 176, 130, 6), "#E3E8EE"),
            P("M484 120C540 108 600 116 684 104", None, "#8C9BAD", 3),
        ], frame="#C9D2DC"),
    ]
    fg = [
        counter(surface="#E3E8EE", lip="#F5F7FA", front="#3D5A99", line="#30497D", planks=False),
        P(rect(98, 222, 156, 96, 8), "#2F2F38"),
        P(rect(106, 230, 140, 72, 4), "#F5F7FA"),
        P(rect(126, 244, 74, 46, 6), "#7B68EE"),
        P(rect(126, 254, 74, 8), "#2F2F38"),
        P(rect(134, 270, 24, 6, 2), "#FFD166"),
        P(poly((222, 244), (238, 272), (206, 272)), "#E5484D"),
        P("M222 252V262", None, WHITE, 3),
        P(rect(166, 300, 20, 14), "#2F2F38"),
        P(rect(500, 296, 110, 22, 4), "#4A4F5C"),
        P(rect(506, 300, 98, 6, 2), "#6D7383"),
        P(rect(632, 284, 46, 34, 6), "#2F2F38"),
        P(rect(638, 290, 34, 10, 2), "#46C37B"),
    ]
    return bg, fg


def movie():
    bulbs = []
    for x0, y0, w0, h0 in ((40, 52, 150, 214), (530, 52, 150, 214)):
        for i in range(8):
            bulbs.append(P(circle(x0 + 8 + i * (w0 - 16) / 7, y0 - 8, 4.5), "#FFD166"))
            bulbs.append(P(circle(x0 + 8 + i * (w0 - 16) / 7, y0 + h0 + 8, 4.5), "#FFD166"))
    bg = [
        wall("#2E2648", "#43386A"),
        P(rect(40, 52, 150, 214, 4), "#1B1436"),
        G(
            P(rect(48, 60, 134, 198), vgrad(60, 258, "#3B2E7E", "#E07A5F")),
            P(circle(116, 150, 44), "#F2A541"),
            P(ellipse(116, 150, 70, 12, -16), None, "#FFE7A8", 4),
            [P(circle(x, y, 2), WHITE) for x, y in ((64, 80), (160, 90), (80, 200), (170, 220), (140, 76))],
            P(rect(60, 230, 110, 10, 4), WHITE, alpha=0.9),
            clip=rect(48, 60, 134, 198),
        ),
        P(rect(530, 52, 150, 214, 4), "#1B1436"),
        G(
            P(rect(538, 60, 134, 198), vgrad(60, 258, "#0F2A44", "#4ECDC4")),
            P(circle(640, 96, 18), "#FFE7A8"),
            P(circle(574, 120, 10), "#F7B6C6"),
            P("M538 258V200L566 176L592 196L618 168L646 192L672 180V258Z", "#1B2340"),
            G(P(rect(-6, -26, 12, 40, 6), WHITE), P(poly((-6, -18), (-14, 8), (-6, 6)), "#E4572E"), P(poly((6, -18), (14, 8), (6, 6)), "#E4572E"), P(poly((-4, 14), (4, 14), (0, 28)), "#FFD166"), tx=600, ty=150, rot=20),
            P(rect(550, 230, 110, 10, 4), WHITE, alpha=0.9),
            clip=rect(538, 60, 134, 198),
        ),
        bulbs,
        P(rect(298, 44, 124, 40, 20), "#E4572E", alpha=0.9),
        P(rect(298, 44, 124, 40, 20), None, "#FF9A7A", 3),
        P(circle(326, 64, 10), None, WHITE, 3),
        P(rect(344, 60, 60, 8, 4), WHITE, alpha=0.9),
    ]
    fg = [
        P(rect(0, 320, W, 80), "#5A3A28"),
        P(rect(0, 320, W, 5), "#7A5238"),
        [P(circle(124 + i * 7, 254 - (i % 3) * 5, 8), "#FFF3C4") for i in range(7)],
        P(poly((120, 254), (166, 254), (160, 322), (126, 322)), WHITE),
        G([P(rect(x, 254, 8, 70), "#E4572E") for x in range(118, 170, 16)], clip=poly((120, 254), (166, 254), (160, 322), (126, 322))),
        P(poly((206, 270), (236, 270), (232, 322), (210, 322)), WHITE, "#BFD3E6", 2, alpha=0.8, stroke_alpha=1),
        P(poly((208, 288), (234, 288), (232, 322), (210, 322)), "#8B5E3C"),
        P("M226 270L232 244", None, "#4ECDC4", 3),
        G(P(rect(0, 0, 50, 22, 3), "#FFD166"), P("M36 0V22", None, "#E0A030", 2, cap="butt"), tx=520, ty=300, rot=-10),
        G(P(rect(0, 0, 50, 22, 3), "#FFD166"), P("M36 0V22", None, "#E0A030", 2, cap="butt"), tx=556, ty=296, rot=8),
        P(ellipse(650, 318, 34, 8), WHITE),
        P(circle(650, 310, 12), "#C8783E"),
        [P(circle(x, y, 2), "#5A3A28") for x, y in ((646, 306), (654, 312), (650, 304))],
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
    "airport": (airport, "nina"),
    "lostfound": (lostfound, "greg"),
    "reservation": (reservation, "lucy"),
    "neighbor": (neighbor, "helen"),
    "homestay": (homestay, "linda"),
    "favor": (favor, "ben"),
    "deadline": (deadline, "karen"),
    "feedback": (feedback, "leo"),
    "flight": (flight, "grace"),
    "fraud": (fraud, "kevin"),
    "noise": (noise, "jake"),
    "movie": (movie, "mia"),
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
