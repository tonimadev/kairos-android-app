plugins {
    id("org.jetbrains.kotlin.jvm")
}

// For pure-Kotlin modules that must never pull in the Android framework or Compose
// (e.g. :core:model). Applying only this plugin, with no `com.android.*` plugin, is
// what makes that a build-time guarantee rather than a convention people can forget.
kotlin {
    jvmToolchain(21)
}
