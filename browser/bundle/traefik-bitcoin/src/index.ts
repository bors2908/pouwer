import {bootstrapTraefikChallenge} from "@widget/traefik";
import {bitcoinSha256Binding} from "@worker/sha256";

bootstrapTraefikChallenge({
    payload: bitcoinSha256Binding,
});
