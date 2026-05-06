import {ResultMessage} from "./models";

interface DeliveryContext {
    container: HTMLElement;
    form: HTMLFormElement;
    hiddenResponse: HTMLInputElement;
}

export function deliverResultToPlugin(context: DeliveryContext, resultMessage: ResultMessage) {
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
