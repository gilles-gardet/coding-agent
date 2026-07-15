---
name: grep
description: Search file contents with grep, portably across Linux and macOS
---

```bash
grep -rn 'PATTERN' .        # recursive search with line numbers
grep -rni 'PATTERN' src     # case-insensitive
grep -rl 'PATTERN' .        # list matching file names only
grep -rnw 'PATTERN' .       # match whole words
grep -rn -- '-x' .          # search a literal that starts with a dash
```

Portability (GNU on Linux vs BSD on macOS):

- Portable flags: `-r -n -i -l -w -c -v -E -F -o`.
- Use `-E` for extended regex instead of the deprecated `egrep`.
- Avoid `-P` (Perl regex) — it is GNU-only and absent on macOS.
- `--include`/`--exclude` are GNU-only; on macOS narrow the file set with `find` first.

Prefer the agent's `Grep` tool over shelling out for content search — it is faster and structured.
