#!/usr/bin/env python3
"""
リリース版 APK (R8 で縮小・難読化したもの) をエミュレータで操作し、
データの保存 (kotlinx.serialization) や Claude API の呼び出し (Anthropic SDK) が
縮小後も壊れていないことを確かめる。CI (emulator ジョブ) から呼ぶ。

    python3 scripts/release_smoke.py <スクリーンショットの保存先>
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "io.github.twatanabe1436.sodateru"
SHOT_DIR = sys.argv[1] if len(sys.argv) > 1 else "."


def adb(*args):
    return subprocess.run(["adb", *args], capture_output=True, text=True).stdout


def dump():
    out = adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    if "dumped" not in out:
        return None
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    try:
        return ET.fromstring(xml[xml.index("<"):])
    except (ValueError, ET.ParseError):
        return None


def find(root, text, contains=False, cls=None):
    if root is None:
        return None
    for node in root.iter("node"):
        if cls and node.get("class") != cls:
            continue
        if cls and not text:
            return node
        t = node.get("text", "")
        d = node.get("content-desc", "")
        if t == text or d == text or (contains and (text in t or text in d)):
            return node
    return None


def wait_for_any(texts, timeout=20, contains=False, cls=None):
    end = time.time() + timeout
    while time.time() < end:
        root = dump()
        for text in texts:
            node = find(root, text, contains, cls)
            if node is not None:
                return text, node
        time.sleep(1)
    shot("failed_" + str(int(time.time())))
    raise SystemExit(f"画面に見つかりません: {texts}")


def wait_for(text, **kw):
    return wait_for_any([text], **kw)[1]


def tap_node(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(1.5)


def tap(text, **kw):
    tap_node(wait_for(text, **kw))


def shot(name):
    png = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout
    with open(f"{SHOT_DIR}/{name}.png", "wb") as f:
        f.write(png)


def main():
    adb("shell", "pm", "clear", PKG)
    adb("shell", "am", "start", "-W", "-n", f"{PKG}/.MainActivity")
    wait_for("レシピを育てよう", contains=True)
    shot("r01_release_empty")

    # サンプル追加 → SQLite に JSON で保存 (シリアライズ) → 一覧・詳細
    tap("サンプルを入れて試してみる")
    tap("山型食パン（サンプル）")
    wait_for("今日の条件で仕込む", contains=True)
    shot("r02_release_detail")

    # アプリを終了して起動し直し、保存したデータを読み戻せること (デシリアライズ)
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", f"{PKG}/.MainActivity")
    wait_for("豚の生姜焼き（サンプル）")

    # Claude API: 無効なキーで確認 → SDK がエラー応答を読み取って日本語のメッセージになること
    tap("設定")
    tap("キーを入力")
    field = wait_for("", cls="android.widget.EditText")
    tap_node(field)
    adb("shell", "input", "text", "sk-ant-invalid-key-for-ci")
    time.sleep(1)
    tap("確認して保存")
    which, _ = wait_for_any(["APIキーが正しくない", "インターネットに接続できません"], timeout=60, contains=True)
    print(f"API key check result: {which}")
    shot("r03_release_api_key_error")

    pid = adb("shell", "pidof", PKG).strip()
    if not pid:
        raise SystemExit("アプリが終了しています")
    print("release smoke OK")


if __name__ == "__main__":
    main()
