---
name: ls
description: List directory contents portably across Linux and macOS
---

```bash
ls -la      # long format, including hidden entries
ls -lh      # human-readable sizes
ls -lt      # sort by modification time, newest first
ls -1       # one entry per line (script-friendly)
```

Portability: `-l -a -h -t -r -1 -R -d -S` are portable across Linux and macOS. Colour flags differ (`--color` is GNU, `-G` is macOS) and are purely cosmetic — omit them.

Never parse `ls` output programmatically (filenames may contain spaces or newlines). Use `find`, `fd`, or a shell glob to enumerate files for scripts, and prefer the agent's `Glob` tool when you just need a list of paths.
