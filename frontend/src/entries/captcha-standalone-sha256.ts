import {JobType} from "../contracts";
import {bootstrapChallengeWidget} from "../core/challenge-widget";
import {sha256PowModule} from "../payloads/sha256";

bootstrapChallengeWidget({
    modules: [sha256PowModule],
    defaultJobType: JobType.POW_TEST_SHA256,
});
