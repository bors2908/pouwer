# @pouwer/widget-traefik

Traefik-specific adapter for the generic challenge widget.

This package wraps `@pouwer/widget-base` and delivers a solved challenge through markup compatible with the Traefik/CrowdSec captcha flow used in the examples.

## What It Does

- Boots `bootstrapChallengeWidget()` with a plugin `PayloadBinding`.
- Serializes the resulting `ResultMessage` to JSON.
- Writes the JSON into the hidden input `#captcha-response`.
- Uses `data-response-field` from `#captcha` as the input name, defaulting to `response`.
- Calls `window[data-callback]` if configured, defaulting to `captchaCallback`.
- Submits `#captcha-form` if no callback exists.

## Pages

Static page templates live under `pages/`:

| File | Purpose |
| --- | --- |
| `pages/challenge.html` | Challenge page used by the Traefik integration. Contains placeholders such as `{{ .FrontendJS }}`, `{{ .FrontendKey }}`, and `{{ .SiteKey }}`. |
| `pages/ban.html` | Ban/remediation page. |

`pouwer-core:server:core-runtime` copies these pages into its static resources during the Gradle build.

## Build

From this directory:

```bash
npm run build
npm run typecheck
```

From repository root:

```bash
npm --workspace @pouwer/widget-traefik run build
```

There is no test script in this package.

## Technical Notes

- This package is intentionally thin. Solver logic belongs to plugin workers, and generic UI/network behavior belongs to `widget-base`.
- The adapter assumes Traefik captcha-page markup. If another edge layer is added, build another adapter instead of adding conditionals here.
