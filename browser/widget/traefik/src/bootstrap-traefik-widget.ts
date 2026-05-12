import type {PayloadBinding} from "@pouwer/core-contracts";
import {bootstrapChallengeWidget} from "@pouwer/widget-base";
import {deliverResultToPlugin, getTraefikDeliveryContext} from "./result-delivery.js";

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
