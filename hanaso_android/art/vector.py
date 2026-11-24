"""イラスト用の小さなベクター描画ライブラリ。

同じ図形のデータから、確認用の SVG と Android の VectorDrawable (XML) を書き出す。
使える図形は VectorDrawable でそのまま表せるものだけにしている:
パス (塗り・線・透明度・線形グラデーション)、グループ (移動・拡大・回転・切り抜き)。
"""

import math


def num(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("", "-0") else s


# ---- 図形をパスに ----

def rect(x, y, w, h, r=0):
    if r <= 0:
        return f"M{num(x)} {num(y)}H{num(x + w)}V{num(y + h)}H{num(x)}Z"
    r = min(r, w / 2, h / 2)
    a = f"A{num(r)} {num(r)} 0 0 1"
    return (
        f"M{num(x + r)} {num(y)}H{num(x + w - r)}{a} {num(x + w)} {num(y + r)}V{num(y + h - r)}"
        f"{a} {num(x + w - r)} {num(y + h)}H{num(x + r)}{a} {num(x)} {num(y + h - r)}V{num(y + r)}"
        f"{a} {num(x + r)} {num(y)}Z"
    )


def ellipse(cx, cy, rx, ry, rot=0):
    """回転できる楕円 (半分ずつの円弧 2 本)"""
    t = math.radians(rot)
    dx, dy = rx * math.cos(t), rx * math.sin(t)
    a = f"A{num(rx)} {num(ry)} {num(rot)} 1 1"
    return f"M{num(cx + dx)} {num(cy + dy)}{a} {num(cx - dx)} {num(cy - dy)}{a} {num(cx + dx)} {num(cy + dy)}Z"


def circle(cx, cy, r):
    return ellipse(cx, cy, r, r)


def poly(*pts):
    return "M" + "L".join(f"{num(x)} {num(y)}" for x, y in pts) + "Z"


def star4(cx, cy, r):
    """きらきら (4 つの角の星)"""
    k = r * 0.28
    return poly((cx, cy - r), (cx + k, cy - k), (cx + r, cy), (cx + k, cy + k), (cx, cy + r), (cx - k, cy + k), (cx - r, cy), (cx - k, cy - k))


# ---- 部品 ----

class Linear:
    """線形グラデーション (座標はそのパスの座標系)"""

    def __init__(self, x1, y1, x2, y2, *stops):
        self.x1, self.y1, self.x2, self.y2 = x1, y1, x2, y2
        self.stops = stops  # (offset, "#RRGGBB")


def vgrad(y1, y2, top, bottom):
    return Linear(0, y1, 0, y2, (0, top), (1, bottom))


class P:
    """パス。fill は色 ("#RRGGBB") か Linear"""

    def __init__(self, d, fill=None, stroke=None, sw=0, alpha=1.0, stroke_alpha=None, cap="round", join="round", evenodd=False):
        self.d, self.fill, self.stroke, self.sw = d, fill, stroke, sw
        self.alpha = alpha
        self.stroke_alpha = alpha if stroke_alpha is None else stroke_alpha
        self.cap, self.join, self.evenodd = cap, join, evenodd


class G:
    """グループ。変形は 拡大 → 回転 → 移動 の順 (VectorDrawable と同じ)。clip はこのグループの座標系のパス"""

    def __init__(self, *children, tx=0, ty=0, s=None, sx=1, sy=1, rot=0, px=0, py=0, clip=None):
        self.children = list(flatten(children))
        self.tx, self.ty = tx, ty
        self.sx, self.sy = (s, s) if s is not None else (sx, sy)
        self.rot, self.px, self.py = rot, px, py
        self.clip = clip


def flatten(items):
    for it in items:
        if it is None:
            continue
        if isinstance(it, (list, tuple)):
            yield from flatten(it)
        else:
            yield it


class Drawing:
    def __init__(self, w, h, *children, dp=None):
        self.w, self.h = w, h
        self.children = list(flatten(children))
        # 表示の大きさ (dp)。指定がなければ半分
        self.dp = dp or (w / 2, h / 2)

    # ---- SVG ----

    def svg(self, extra=""):
        out = _Svg()
        body = "".join(out.node(c) for c in self.children)
        return (
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{num(self.w)}" height="{num(self.h)}" '
            f'viewBox="0 0 {num(self.w)} {num(self.h)}"><defs>{"".join(out.defs)}</defs>{body}{extra}</svg>'
        )

    # ---- VectorDrawable ----

    def vector(self):
        lines = [
            '<?xml version="1.0" encoding="utf-8"?>',
            "<!-- art/build.py で生成。手で直さない -->",
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
            '    xmlns:aapt="http://schemas.android.com/aapt"',
            f'    android:width="{num(self.dp[0])}dp"',
            f'    android:height="{num(self.dp[1])}dp"',
            f'    android:viewportWidth="{num(self.w)}"',
            f'    android:viewportHeight="{num(self.h)}">',
        ]
        for c in self.children:
            _vd(c, lines, 1)
        lines.append("</vector>")
        return "\n".join(lines) + "\n"


class _Svg:
    def __init__(self):
        self.defs = []
        self.n = 0

    def _id(self, prefix):
        self.n += 1
        return f"{prefix}{self.n}"

    def paint(self, p):
        if p is None:
            return "none"
        if isinstance(p, Linear):
            gid = self._id("g")
            stops = "".join(f'<stop offset="{num(o)}" stop-color="{c}"/>' for o, c in p.stops)
            self.defs.append(
                f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="{num(p.x1)}" y1="{num(p.y1)}" '
                f'x2="{num(p.x2)}" y2="{num(p.y2)}">{stops}</linearGradient>'
            )
            return f"url(#{gid})"
        return p

    def node(self, n):
        if isinstance(n, P):
            a = [f'd="{n.d}"', f'fill="{self.paint(n.fill)}"']
            if n.fill is not None and n.alpha != 1:
                a.append(f'fill-opacity="{num(n.alpha)}"')
            if n.evenodd:
                a.append('fill-rule="evenodd"')
            if n.stroke is not None and n.sw > 0:
                a += [f'stroke="{self.paint(n.stroke)}"', f'stroke-width="{num(n.sw)}"',
                      f'stroke-linecap="{n.cap}"', f'stroke-linejoin="{n.join}"']
                if n.stroke_alpha != 1:
                    a.append(f'stroke-opacity="{num(n.stroke_alpha)}"')
            return f"<path {' '.join(a)}/>"
        inner = "".join(self.node(c) for c in n.children)
        if n.clip:
            cid = self._id("c")
            self.defs.append(f'<clipPath id="{cid}"><path d="{n.clip}"/></clipPath>')
            inner = f'<g clip-path="url(#{cid})">{inner}</g>'
        t = []
        if n.tx or n.ty:
            t.append(f"translate({num(n.tx)} {num(n.ty)})")
        if n.rot or n.sx != 1 or n.sy != 1:
            t.append(f"translate({num(n.px)} {num(n.py)})")
            if n.rot:
                t.append(f"rotate({num(n.rot)})")
            if n.sx != 1 or n.sy != 1:
                t.append(f"scale({num(n.sx)} {num(n.sy)})")
            t.append(f"translate({num(-n.px)} {num(-n.py)})")
        return f'<g transform="{" ".join(t)}">{inner}</g>' if t else f"<g>{inner}</g>"


def _vd_gradient(p, lines, pad, attr):
    lines.append(f'{pad}    <aapt:attr name="android:{attr}">')
    lines.append(
        f'{pad}        <gradient android:type="linear" android:startX="{num(p.x1)}" android:startY="{num(p.y1)}" '
        f'android:endX="{num(p.x2)}" android:endY="{num(p.y2)}">'
    )
    for o, c in p.stops:
        lines.append(f'{pad}            <item android:offset="{num(o)}" android:color="{c}"/>')
    lines.append(f"{pad}        </gradient>")
    lines.append(f"{pad}    </aapt:attr>")


def _vd(n, lines, depth):
    pad = "    " * depth
    if isinstance(n, P):
        a = [f'android:pathData="{n.d}"']
        grads = []
        if isinstance(n.fill, Linear):
            grads.append(("fillColor", n.fill))
        elif n.fill is not None:
            a.append(f'android:fillColor="{n.fill}"')
        if n.fill is not None and n.alpha != 1:
            a.append(f'android:fillAlpha="{num(n.alpha)}"')
        if n.evenodd:
            a.append('android:fillType="evenOdd"')
        if n.stroke is not None and n.sw > 0:
            if isinstance(n.stroke, Linear):
                grads.append(("strokeColor", n.stroke))
            else:
                a.append(f'android:strokeColor="{n.stroke}"')
            a += [f'android:strokeWidth="{num(n.sw)}"', f'android:strokeLineCap="{n.cap}"', f'android:strokeLineJoin="{n.join}"']
            if n.stroke_alpha != 1:
                a.append(f'android:strokeAlpha="{num(n.stroke_alpha)}"')
        if not grads:
            lines.append(f"{pad}<path {' '.join(a)}/>")
        else:
            lines.append(f"{pad}<path {' '.join(a)}>")
            for attr, g in grads:
                _vd_gradient(g, lines, pad, attr)
            lines.append(f"{pad}</path>")
        return
    a = []
    if n.tx:
        a.append(f'android:translateX="{num(n.tx)}"')
    if n.ty:
        a.append(f'android:translateY="{num(n.ty)}"')
    if n.sx != 1:
        a.append(f'android:scaleX="{num(n.sx)}"')
    if n.sy != 1:
        a.append(f'android:scaleY="{num(n.sy)}"')
    if n.rot:
        a.append(f'android:rotation="{num(n.rot)}"')
    if n.px:
        a.append(f'android:pivotX="{num(n.px)}"')
    if n.py:
        a.append(f'android:pivotY="{num(n.py)}"')
    lines.append(f"{pad}<group{(' ' + ' '.join(a)) if a else ''}>")
    if n.clip:
        lines.append(f'{pad}    <clip-path android:pathData="{n.clip}"/>')
    for c in n.children:
        _vd(c, lines, depth + 1)
    lines.append(f"{pad}</group>")
