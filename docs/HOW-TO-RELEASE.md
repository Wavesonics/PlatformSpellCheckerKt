# How to release

- Create tag "vX.Y.Z"
- Push tag
- GitHub Actions will build and publish release

## API docs

The Dokka API docs (with the `Module.md` recipes) are served by GitHub Pages from the root of the
**`gh-pages`** branch:

- Reference: https://darkrock-studios.github.io/PlatformSpellCheckerKt/

Pushing a `vX.Y.Z` tag triggers `.github/workflows/deploy-docs.yml`, which builds the docs and
force-replaces the `gh-pages` branch contents (the branch is created automatically on the first run).
It can also be run on demand from the Actions tab (**Deploy Docs → Run workflow**). To generate them
locally:

```bash
./gradlew updateDocs       # generates docs/api/ (git-ignored; published only to gh-pages)
```

> **Repo setting:** after the first run, set **Settings → Pages → Source** to *Deploy from a branch*,
> branch **`gh-pages`** / **`/ (root)`**.