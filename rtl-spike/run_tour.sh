#!/bin/bash
# Runs the RTL UI tour headless (gamescope) for each locale given, e.g. ./run_tour.sh en ar
HERE=$(cd "$(dirname "$0")" && pwd)
S=${S:-$HERE/work}
JAR=${JAR:-$HERE/../desktop/build/libs/Mindustry.jar}
JAVA=${JAVA:-java}
for loc in "$@"; do
    D=$S/mtest_$loc
    rm -rf "$D"
    mkdir -p "$D/Mindustry/mods"
    cp -r "$HERE/tour-mod" "$D/Mindustry/mods/rtltest"
    echo "== $loc"
    XDG_DATA_HOME=$D timeout 300 gamescope --backend headless -W 1280 -H 720 -w 1280 -h 720 -- \
        "$JAVA" -Duser.language=$loc -jar "$JAR" > "$S/tour_$loc.log" 2>&1
    echo "exit $?"
    cat "$D/Mindustry/rtlshots/log.txt" 2>/dev/null | grep -v "^shot\|^ok" | head -20
    ls "$D/Mindustry/rtlshots/" 2>/dev/null | grep -c png
done
