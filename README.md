# Chat-multiple-client--text-audio-activity-
# Bienestar Universitario
## Commit suggested structure

### Format

`<type>(<optional scope>): <short description>`


- The `<type>` describes the purpose of the change.
- The `<scope>` (optional) specifies the area of the codebase affected.
- The `<short description>` summarizes the change in a concise way.

### Common Types

| Type     | Description                                      |
|----------|--------------------------------------------------|
| `feat`   | Introduces a new feature                         |
| `fix`    | Fixes a bug or issue                             |
| `docs`   | Updates or improves documentation                |
| `style`  | Code formatting changes (no logic impact)        |
| `refactor` | Code restructuring without changing behavior   |
| `test`   | Adds or updates tests                            |
| `chore`  | Maintenance tasks (e.g., configs, dependencies)  |

### Examples

```bash
feat(auth): add JWT-based login
fix(api): resolve user serialization error
docs(readme): update setup instructions
style(frontend): apply consistent spacing
refactor(models): simplify client-reservation relationship

