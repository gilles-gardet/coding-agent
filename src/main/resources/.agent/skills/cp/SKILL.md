---
name: cp
description: Copy files and directories with cp
---

```bash
cp file dest     # copy a file
cp -R dir dest   # copy a directory tree
cp -i src dest   # prompt before overwriting
cp -p src dest   # preserve mode, ownership, and timestamps
```

Use `-R` for recursion — it is portable across Linux and macOS. Avoid `-r`, which behaves differently on BSD/macOS. `-i -f -p -v` are portable.

`cp` overwrites the destination silently by default; use `-i` when it may already exist. Quote paths that contain spaces.
