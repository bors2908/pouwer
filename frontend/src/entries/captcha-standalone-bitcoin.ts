import {JobType} from "../contracts";
import {bootstrapChallengeWidget} from "../core/challenge-widget";
import {bitcoinSha256Module} from "../payloads/sha256";

bootstrapChallengeWidget({
    modules: [{...bitcoinSha256Module, enabled: true}],
    defaultJobType: JobType.BITCOIN_RPC_SHA256,
});
