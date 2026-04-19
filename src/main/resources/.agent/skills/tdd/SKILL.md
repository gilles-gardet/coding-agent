---
name: tdd
description: Test-Driven Development workflow for implementing new features or fixing bugs
---

When implementing a new feature or fixing a bug, follow the Red-Green-Refactor cycle strictly.

## Red Phase — Write a failing test first

1. Create or identify the test class for the feature
2. Write the smallest possible test that describes the expected behavior
3. Run the test and confirm it fails (a compilation error counts as red)
4. Do NOT write any production code before seeing the test fail

## Green Phase — Make the test pass

5. Write the minimum production code to make the failing test pass
6. Do not over-engineer — just enough to pass the test
7. Run the test suite and confirm the new test is green
8. Ensure no existing tests are broken

## Refactor Phase — Clean up

9. Improve the code structure without changing behavior
10. Remove duplication, improve naming, apply project conventions (final, var, guard clauses)
11. Re-run the test suite after each refactoring step to confirm everything stays green

## Project Test Conventions

- Use camelCase for test method names (no underscores)
- Always add `@DisplayName` with 🎉 for success cases and 💩 for failure cases
- Use AssertJ: `Assertions.assertThat(...)` — never JUnit assertions
- Declare test variables with `final` when they won't be reassigned
- Never use static imports in tests — use fully qualified class names

## Rules

- Never skip the Red phase — always see the test fail first before writing production code
- Keep each test focused on a single behavior
- When unsure what to test next, pick the simplest failing case
