#!/bin/bash
# Run pw.x in every directory below ROOT that has a pw.in but no finished pw.out.
#
#   usage: bash tools/run_all.sh ROOT [NPROC]
#     e.g. bash tools/run_all.sh runs/surface/TiO2_7L_2x2/empty 8
#          bash tools/run_all.sh runs/surface/TiO2_7L_2x2_relaxed 8
#
# Finished = pw.out contains "JOB DONE" and no "convergence NOT achieved" /
# "maximum number of steps". Re-running the script skips finished directories,
# so it can be stopped (Ctrl-C) and restarted. Set PW="pw.x" / MPI="mpirun" to override.
set -u
root=${1:?usage: run_all.sh ROOT [NPROC]}
np=${2:-4}
PW=${PW:-pw.x}
MPI=${MPI:-mpirun}
export OMP_NUM_THREADS=1

mapfile -t dirs < <(find "$root" -name pw.in -printf '%h\n' | sort)
echo "${#dirs[@]} calculations under $root (np=$np)"
for d in "${dirs[@]}"; do
  if [ -f "$d/pw.out" ] && grep -q "JOB DONE" "$d/pw.out" \
     && ! grep -q -E "convergence NOT achieved|maximum number of steps" "$d/pw.out"; then
    echo "skip  $d (finished)"
    continue
  fi
  echo "run   $d  $(date '+%F %T')"
  (cd "$d" && $MPI -np "$np" "$PW" -in pw.in > pw.out 2> pw.err)
  if grep -q -E "convergence NOT achieved|maximum number of steps" "$d/pw.out"; then
    echo "WARN  $d did not converge (see pw.out)"
  fi
  # wavefunction/charge files are large and not needed afterwards
  rm -rf "$d/tmp"
done
echo "done $(date '+%F %T')"
