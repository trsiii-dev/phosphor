plugins {
    application
}

group = "dev.trsiii"

// Build number shown in the page footer (PHOSPHOR 1.1). Set only here.
version = "1.1"

repositories {
    mavenCentral()
}

java {
    // Uses JDK 25 no matter which JDK runs Gradle. SDKMAN installs are found
    // automatically, and the build fails if there is no JDK 25.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // YAML config parsing.
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    // Gradle 9 no longer adds the JUnit launcher automatically.
    testRuntimeOnly(libs.junit.launcher)
}

application {
    mainClass = "dev.trsiii.phosphor.Main"
}

tasks.withType<JavaCompile>().configureEach {
    // Source files are UTF-8 on every OS.
    options.encoding = "UTF-8"
}

// Writes the version into build.properties, which BuildInfo reads at startup.
val buildVersion = version.toString()
tasks.processResources {
    // Declared as an input so a version change reruns the task.
    inputs.property("version", buildVersion)
    filesMatching("**/build.properties") {
        expand(mapOf("version" to buildVersion))
    }
}

tasks.test {
    useJUnitPlatform()
    // Pages are painted into images, so the tests must never need a display.
    systemProperty("java.awt.headless", "true")
}
