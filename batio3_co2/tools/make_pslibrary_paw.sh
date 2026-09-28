#!/bin/bash
# Generate pslibrary 1.0.0 PAW data sets for Ba, Ti, O, C with ld1.x
# (useful when SSSP / pslibrary files are not at hand).
#
#   usage: bash make_pslibrary_paw.sh [output_dir]      (needs ld1.x in PATH)
#
# Files produced: Ba.pbe-spn-, Ti.pbe-spn-, O.pbe-n-, C.pbe-n-kjpaw_psl.1.0.0.UPF
# (= btoco2.qe.PSEUDOS_PSL). Long data lines are re-wrapped so that recent
# pw.x versions can read them.
set -euo pipefail
out=${1:-pseudo}
here=$(cd "$(dirname "$0")" && pwd)
mkdir -p "$out"
cd "$out"
curl -sSL -o paw_ps_high.job https://raw.githubusercontent.com/dalcorso/pslibrary/master/paw_ps_high.job

fct=pbe; gfun=PBE; nrel=1
for tag in 'C.$fct-n-kjpaw_psl.1.0.0' 'O.$fct-n-kjpaw_psl.1.0.0' \
           'Ti.$fct-spn-kjpaw_psl.1.0.0' 'Ba.$fct-spn-kjpaw_psl.1.0.0'; do
  name=$(eval echo "$tag")
  # extract the here-document for this element and expand $fct/$gfun/$nrel
  awk -v t="cat > $tag.in << EOF" 'index($0,t)==1{f=1;next} f&&/^EOF/{f=0} f' paw_ps_high.job > tmpl.in
  eval "cat > $name.in << EOF
$(cat tmpl.in)
EOF"
  ld1.x < "$name.in" > "$name.ld1.out" 2>&1
  echo "generated $name.UPF"
done
python3 "$here/wrap_upf_lines.py" ./*.UPF
rm -f tmpl.in ld1.wfc ld1ps.wfc ld1.test vx.pot paw_ps_high.job
