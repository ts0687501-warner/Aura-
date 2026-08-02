pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AURA"

include(":app")
include(":core-voice")
include(":core-brain")
include(":core-ui-theme")
// Phase 2+: uncomment as modules are built
// include(":core-memory")
// include(":core-vision")
// include(":core-automation")
// include(":core-phonecontrol")
// include(":core-plugins")
// include(":core-security")
