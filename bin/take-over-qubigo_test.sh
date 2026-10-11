#!/usr/bin/env bash
set -euo pipefail

script="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/take-over-qubigo"
work="$(mktemp -d)"
trap 'rm -rf -- "$work"' EXIT
source="$work/source"
destination="$work/snapshot"
mkdir -p "$source/docs/iterations/evidence" "$source/.assembly-lines/.machines/planner"
printf 'vision\n' > "$source/docs/vision.md"
printf 'backlog\n' > "$source/docs/backlog.md"
printf 'iteration\n' > "$source/docs/iterations/001.md"
printf 'test output\n' > "$source/docs/iterations/evidence/test.txt"
printf '{}\n' > "$source/.assembly-lines/.machines/planner/config.json"
printf 'private application code\n' > "$source/application.txt"
ln -s "$source" "$work/q"

"$script" "$work/q" "$destination"
diff -r "$source/docs" "$destination/docs"
diff -r "$source/.assembly-lines" "$destination/.assembly-lines"
test ! -e "$destination/application.txt"
printf 'updated vision\n' > "$source/docs/vision.md"
printf 'keep\n' > "$destination/README.md"
"$script" "$work/q" "$destination"
cmp "$source/docs/vision.md" "$destination/docs/vision.md"
test -f "$destination/README.md"

expect_failure() {
  if "$script" "$@" > "$work/error.log" 2>&1; then
    printf 'Expected failure for: %s\n' "$*" >&2
    exit 1
  fi
}
expect_failure "$source" "$source"
expect_failure "$source" "$source/nested"
expect_failure "$source" "$work"
ln -s "$source/application.txt" "$source/docs/iterations/leak"
expect_failure "$source" "$work/symlink-export"
test ! -e "$work/symlink-export"
rm "$source/docs/iterations/leak"
ln -s "$source/docs/vision.md" "$destination/docs/leak"
expect_failure "$source" "$destination"
rm "$source/docs/backlog.md"
expect_failure "$source" "$work/incomplete"
test ! -e "$work/incomplete"
printf 'All take-over-qubigo tests passed.\n'
