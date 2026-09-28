#!/usr/bin/env python3
"""Re-wrap very long numeric lines in UPF files (in place).

UPF files written by some ld1.x versions contain data lines longer than
1024 characters, which the XML reader of recent pw.x versions rejects with
"xmlr_opentag: severe error, line too long". Numeric data are
whitespace-separated, so wrapping them (4 numbers per line) is harmless.

usage: python wrap_upf_lines.py *.UPF
"""
import sys

for path in sys.argv[1:]:
    out, changed = [], 0
    with open(path) as f:
        for line in f:
            s = line.strip()
            if len(line) > 200 and s and not s.startswith("<"):
                tok = s.split()
                out += ["  " + "  ".join(tok[i:i + 4]) + "\n" for i in range(0, len(tok), 4)]
                changed += 1
            else:
                out.append(line)
    with open(path, "w") as f:
        f.writelines(out)
    print(f"{path}: {changed} lines re-wrapped")
