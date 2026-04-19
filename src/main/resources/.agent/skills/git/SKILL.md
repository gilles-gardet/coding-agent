---
name: git
description: Git workflow for committing, branching, worktrees, and history rewriting
---

When working with git, always start by understanding the current state before taking any action:

```bash
git status
git log --oneline -10
```

Read the recent commit messages to understand the style in use before writing any new commit message.

## Starting a new feature or task

When asked to implement a new feature or work on a task that may take several steps, **create a worktree** instead of switching branches. Worktrees let you work on an isolated branch without disturbing the current working directory.

```bash
git worktree add ../project-feat-name feat/feature-name
```

Place the worktree in a sibling directory (never inside the repo). When the feature is merged, clean it up:

```bash
git worktree remove ../project-feat-name
```

Use a regular branch checkout only for quick, single-step fixes where context switching is not an issue.

## Committing changes

When you have changes ready to commit, stage only the files that belong to the same logical change — never use `git add .` or `git add -A`. Group unrelated changes into separate commits.

Write commit messages in Conventional Commits format:

```
<type>(<scope>): <short summary>
```

- **type**: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`
- **summary**: imperative mood, lowercase, no trailing period, under 72 characters
- Use the commit body to explain WHY, not WHAT

## Fixing a mistake in the last commit

When you realize a file is missing from the last commit, or the message is wrong, amend it — but only if that commit has not been pushed yet:

```bash
git add forgotten-file
git commit --amend --no-edit        # keep the message
git commit --amend -m "new message" # change the message
```

If the commit has already been pushed to a shared branch, create a new follow-up commit instead.

## Fixing a mistake in an older commit

When a file or change belongs to a commit that is not the last one, use fixup and autosquash:

```bash
git add path/to/file
git commit --fixup=<sha-of-target-commit>
git rebase --autosquash <sha-of-target-commit>~1
```

Use `git log --oneline` to find the target SHA before starting.

## Cleaning up history before opening a PR

When a feature branch has accumulated noisy or redundant commits, use interactive rebase to produce a clean, readable history before merging:

```bash
git rebase -i main
```

In the editor, use these actions:
- `pick` — keep the commit as-is
- `reword` — keep the commit but edit its message
- `squash` / `fixup` — merge into the previous commit (`squash` keeps both messages, `fixup` discards the squashed one)
- `drop` — remove the commit entirely
- Reorder lines to reorder commits in history

When the rebase pauses on an `edit` commit, make your changes then:

```bash
git add ...
git commit --amend --no-edit
git rebase --continue
```

If anything goes wrong, abort immediately — git restores the original state:

```bash
git rebase --abort
```

After rewriting a branch, push with `--force-with-lease` (never `--force`) to protect against overwriting someone else's push:

```bash
git push --force-with-lease
```

## Rules

- Never rewrite history on `main` or any branch shared with other developers
- Never force-push to `main`
- Never skip hooks (`--no-verify`) unless explicitly instructed
- Never commit files that contain secrets, credentials, or environment variables
- Always confirm with the user before any destructive operation (`reset --hard`, `branch -D`, `rebase --onto`)
