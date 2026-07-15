---
name: cat
description: Print and concatenate file contents with cat
---

```bash
cat file            # print a file
cat -n file         # print with line numbers
cat a b > combined  # concatenate files into one
```

`-n` (number all lines) and `-b` (number non-blank lines) are portable across Linux and macOS.

For large files, read only what you need with `head`, `tail`, or a ranged read instead of dumping the whole file. Prefer the agent's read tool over `cat` when inspecting a file — it is structured and paginated.
