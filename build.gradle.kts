import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "io.github.vadimtoptunov"
// Overridable by CI at release time: ./gradlew publishPlugin -PpluginVersion=1.7.0
version = (findProperty("pluginVersion") as String?) ?: "1.7.0"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin — https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.6")
    type.set("IC") // IntelliJ IDEA Community. The plugin is platform-only, so it also loads in Android Studio.
    plugins.set(listOf())
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        version.set(project.version.toString())
        sinceBuild.set("232")
        untilBuild.set("") // No upper bound — the plugin uses only stable, long-lived platform APIs.
    }

    test {
        useJUnitPlatform()
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // Release gate: fail the build (and therefore the publish) if the plugin uses API that is
    // scheduled for removal or is otherwise incompatible — checked against the oldest supported
    // and a recent IDE. Run via `./gradlew runPluginVerifier` before publishing.
    runPluginVerifier {
        ideVersions.set(listOf("IC-232.10335.12", "IC-252.28539.13"))
        failureLevel.set(
            listOf(
                FailureLevel.COMPATIBILITY_PROBLEMS,
                FailureLevel.INVALID_PLUGIN,
                FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
            )
        )
    }
}
