import {bootstrapTraefikChallenge} from "@pouwer/widget-traefik";
import {sha256PowBinding} from "@pouwer/worker-sha256";

bootstrapTraefikChallenge({
    payload: sha256PowBinding,
});
