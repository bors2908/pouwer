import {bootstrapTraefikChallenge} from "@pouwer/widget-traefik";
import {bitcoinSha256Binding} from "@pouwer/worker-sha256";

bootstrapTraefikChallenge({
    payload: bitcoinSha256Binding,
});
