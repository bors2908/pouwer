import {bootstrapChallengeWidget} from "../base/challenge-widget";
import type {PayloadBinding} from "../base/payload-binding";
import {deliverResultToPlugin, getTraefikDeliveryContext} from "./result-delivery";

export interface TraefikChallengeConfig {
    payload: PayloadBinding;
}

export function bootstrapTraefikChallenge(config: TraefikChallengeConfig) {
    bootstrapChallengeWidget({
        payload: config.payload,
        onSolved: (result) => {
            deliverResultToPlugin(getTraefikDeliveryContext(), result);
        },
        successMessage: "Result handed back to Traefik",
    });
}
