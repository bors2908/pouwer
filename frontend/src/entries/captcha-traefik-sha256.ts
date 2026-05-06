import {sha256PowBinding} from "../payloads/sha256/binding";
import {bootstrapTraefikChallenge} from "../widget/traefik/bootstrap-traefik-widget";

bootstrapTraefikChallenge({
    payload: sha256PowBinding,
});
