---
name: fd
description: Find files and directories by name or pattern with fd, with a portable find fallback
---

`fd` finds files and directories by name. The search pattern is a regex by default.

```bash
fd PATTERN            # entries matching PATTERN under the current directory
fd -e md             # by extension
fd -t f PATTERN      # files only (-t d for directories)
fd -H PATTERN        # include hidden files
fd -I PATTERN        # do not respect .gitignore
fd -g '*.txt'        # treat the pattern as a glob instead of a regex
```

`fd` is not guaranteed to be installed on every Linux or macOS host. Check first, and fall back to POSIX `find` when it is missing:

```bash
command -v fd >/dev/null && fd -t f PATTERN || find . -type f -name '*PATTERN*'
```

Prefer the agent's `Glob` tool over shelling out when a filename/glob match is all you need.
