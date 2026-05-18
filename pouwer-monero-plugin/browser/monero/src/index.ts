import {bootstrapTraefikChallenge} from "@pouwer/widget-traefik";
import {randomxBinding} from "@pouwer/worker-monero";

bootstrapTraefikChallenge({
    payload: randomxBinding,
});
