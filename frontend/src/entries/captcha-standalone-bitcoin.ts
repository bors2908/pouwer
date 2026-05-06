import {JobType} from "../contracts";
import {bootstrapChallengeWidget} from "../core/challenge-widget";
import {bitcoinSha256Module} from "../lib/sha256/module";

bootstrapChallengeWidget({
    modules: [bitcoinSha256Module],
    defaultJobType: JobType.BITCOIN_RPC_SHA256,
});
