import {bootstrapTraefikChallenge} from "@widget/traefik";
import {sha256PowBinding} from "@worker/sha256";

bootstrapTraefikChallenge({
    payload: sha256PowBinding,
});
