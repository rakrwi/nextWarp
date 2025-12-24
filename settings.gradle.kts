plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "nextWarp"

include("api")
include("common")
include("paper")
include("velocity")