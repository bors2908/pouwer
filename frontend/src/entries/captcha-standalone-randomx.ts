import {JobType} from "../contracts";
import {bootstrapChallengeWidget} from "../core/challenge-widget";
import {randomxModule} from "../payloads/randomx";

bootstrapChallengeWidget({
    modules: [randomxModule],
    defaultJobType: JobType.MONERO_RANDOMX,
});
