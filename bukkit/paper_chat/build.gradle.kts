// Paper-only chat + dialog support.
//
// This module deliberately does NOT depend on purpur-api like :bukkit does. It compiles
// against a modern paper-api only, which is what gives it AsyncChatEvent, ChatRenderer,
// the Dialog API and an Audience-aware Player. Everything in here is loaded reflectively
// at runtime and is simply absent on servers that do not have these classes.
//
// Do not add a repositories { } block here. Repositories are declared centrally in
// settings.gradle.kts, and a project-level block replaces them instead of adding to
// them, which breaks resolution of the transitive dependencies inherited from :shared.
dependencies {
    implementation(projects.bukkit)
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks.compileJava {
    options.release.set(21)
}
