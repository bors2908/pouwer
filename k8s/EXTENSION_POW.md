# Browser challenge extension point (captcha -> custom PoW)

The stable adapter is the **Traefik middleware name**:

- `browser-challenge-adapter` in `k8s/edge-poc/templates/middlewares.yaml`

Today it is implemented with the stock CrowdSec captcha flow (decision type `captcha` from CrowdSec profiles/scenarios). To switch to custom browser PoW later:

1. Keep CrowdSec detection/scenarios/profiles unchanged.
2. Keep route wiring unchanged (IngressRoute still references `browser-challenge-adapter`).
3. Replace the middleware implementation behind `browser-challenge-adapter`:
   - either custom captcha provider mode in the CrowdSec plugin (`captchaProvider: custom` + custom validate endpoint), or
   - a custom challenge middleware/service with the same adapter name.

This preserves the detection model and only swaps the browser challenge mechanism at the edge-remediation layer.

