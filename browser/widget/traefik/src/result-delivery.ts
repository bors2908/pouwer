import type {ResultMessage} from "@core/contracts";

export interface TraefikDeliveryContext {
    container: HTMLElement;
    form: HTMLFormElement;
    hiddenResponse: HTMLInputElement;
}

export function getTraefikDeliveryContext(): TraefikDeliveryContext {
    const container = document.getElementById("captcha");
    const form = document.getElementById("captcha-form");
    const hiddenResponse = document.getElementById("captcha-response");

    if (!container || !(form instanceof HTMLFormElement) || !(hiddenResponse instanceof HTMLInputElement)) {
        throw new Error("Traefik captcha result markup is missing.");
    }

    return {container, form, hiddenResponse};
}

export function deliverResultToPlugin(context: TraefikDeliveryContext, resultMessage: ResultMessage) {
    const responseField = context.container.dataset.responseField || "response";
    const serialized = JSON.stringify(resultMessage);

    context.hiddenResponse.name = responseField;
    context.hiddenResponse.value = serialized;

    const callbackName = context.container.dataset.callback || "captchaCallback";
    const callback = Reflect.get(window, callbackName);

    if (typeof callback === "function") {
        callback(serialized);
        return;
    }

    context.form.submit();
}
