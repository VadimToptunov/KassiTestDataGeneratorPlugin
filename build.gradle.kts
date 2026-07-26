plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "io.github.vadimtoptunov"
// Overridable by CI at release time: ./gradlew publishPlugin -PpluginVersion=1.0.1
version = (findProperty("pluginVersion") as String?) ?: "1.0.0"

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
}
