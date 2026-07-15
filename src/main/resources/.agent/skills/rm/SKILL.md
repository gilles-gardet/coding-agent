---
name: rm
description: Delete files and directories with rm — REQUIRES explicit user confirmation before every use
---

`rm` permanently deletes files and directories. There is no undo.

**Before running any `rm` command you MUST ask the user for explicit permission first.** Show the exact command and the absolute path(s) it will delete, then wait for the user's approval. Never run `rm` on your own initiative, and never as part of a larger piped or chained command without separate confirmation.

```bash
rm file      # remove a single file
rm -r dir    # remove a directory tree
rm -i target # prompt for each removal
```

`-r -f -i -v` are portable across Linux and macOS.

Rules:

- Resolve and display the absolute path before deleting anything.
- Never run `rm -rf` on a path you have not confirmed with the user.
- Never delete outside the working directory without explicit approval.
- Refuse commands that risk mass deletion: `rm -rf /`, `rm -rf ~`, or an unglobbed variable/`*` at a directory root.
- Prefer `mv` to a trash/backup location over `rm` when the deletion is reversible-by-design.
