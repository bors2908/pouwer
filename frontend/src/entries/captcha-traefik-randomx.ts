import {randomxBinding} from "../payloads/randomx/binding";
import {bootstrapTraefikChallenge} from "../widget/traefik/bootstrap-traefik-widget";

bootstrapTraefikChallenge({
    payload: randomxBinding,
});
