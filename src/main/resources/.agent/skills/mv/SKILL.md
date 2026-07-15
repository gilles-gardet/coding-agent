---
name: mv
description: Move or rename files and directories with mv
---

```bash
mv old new       # rename
mv file dir/     # move into a directory
mv -i src dest   # prompt before overwriting an existing destination
mv -n src dest   # never overwrite (no-clobber)
mv -v src dest   # explain what is being moved
```

`-i -f -n -v` are portable across Linux and macOS.

`mv` overwrites the destination silently by default. When the destination may already exist, use `-i` or `-n`. Always quote paths that contain spaces, and confirm the destination directory exists before moving into it.
