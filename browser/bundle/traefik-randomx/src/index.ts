import {bootstrapTraefikChallenge} from "@pouwer/widget-traefik";
import {randomxBinding} from "@pouwer/worker-randomx";

bootstrapTraefikChallenge({
    payload: randomxBinding,
});
