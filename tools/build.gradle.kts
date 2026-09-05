// Offline asset generators: launcher icon drawing. Plain Kotlin on the JVM,
// no Android, no third-party libraries: icons write PNGs with ImageIO.
// Outputs are committed; these tasks only run when a design deliberately
// changes. :tools:checkIcons fails the build if the committed PNGs drift
// from a fresh regeneration, so no hand-edited icon can ride into a release.
plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    // Match the app: build with whatever JDK runs Gradle (the Studio JBR),
    // emit Java 17 bytecode.
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.register<JavaExec>("makeIcons") {
    group = "tools"
    description = "Regenerate the launcher icon set in app/src/main/res."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.puzzlet.tools.MakeIconsKt"
    args = listOf(rootDir.absolutePath)
}

tasks.register<JavaExec>("checkIcons") {
    group = "tools"
    description = "Verify the committed launcher icons match a fresh regeneration."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.puzzlet.tools.MakeIconsKt"
    args = listOf(rootDir.absolutePath, "--check")
}

tasks.register<JavaExec>("makeSounds") {
    group = "tools"
    description = "Regenerate the four sound assets in app/src/main/res/raw."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.puzzlet.tools.SoundGenKt"
    args = listOf(rootDir.absolutePath)
}

tasks.register<JavaExec>("checkSounds") {
    group = "tools"
    description = "Verify the committed sound assets match a fresh regeneration."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.puzzlet.tools.SoundGenKt"
    args = listOf(rootDir.absolutePath, "--check")
}


tasks.register<JavaExec>("makeArt") {
    group = "tools"
    description = "Regenerate the store art (feature graphic, store icon)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.puzzlet.tools.MakeArtKt"
    args = listOf(rootDir.absolutePath)
}
