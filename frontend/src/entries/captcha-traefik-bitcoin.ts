import {bitcoinSha256Binding} from "../payloads/sha256/binding";
import {bootstrapTraefikChallenge} from "../widget/traefik/bootstrap-traefik-widget";

bootstrapTraefikChallenge({
    payload: bitcoinSha256Binding,
});
