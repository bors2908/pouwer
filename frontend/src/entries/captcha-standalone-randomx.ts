import {JobType} from "../contracts";
import {bootstrapChallengeWidget} from "../core/challenge-widget";
import {randomxModule} from "../lib/randomx/module";

bootstrapChallengeWidget({
    modules: [randomxModule],
    defaultJobType: JobType.MONERO_RANDOMX,
});
