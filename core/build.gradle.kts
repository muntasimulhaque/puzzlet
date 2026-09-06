// The rules: cut, scenes, ladder, board and tray math. Plain Kotlin on the
// JVM, no Android plugin and no Android imports anywhere in this module, so
// the app and the offline generators in :tools share one source of truth and
// the compiler enforces the purity the architecture promises (AGENTS.md,
// Architecture). Every rule here is testable without a device.
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
