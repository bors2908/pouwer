import {bootstrapTraefikChallenge} from "@widget/traefik";
import {randomxBinding} from "@worker/randomx";

bootstrapTraefikChallenge({
    payload: randomxBinding,
});
