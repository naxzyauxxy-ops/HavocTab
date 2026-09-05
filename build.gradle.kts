plugins {
    id("havoctab.parent")
}

allprojects {
    group = "dev.havoc"
    version = "6.1.2-havoc.1"
    description = "HavocTab - all-in-one tablist, nametag and scoreboard suite"

    ext.set("id", "havoctab")
    ext.set("website", "https://github.com/NaxzyAuxxy/HavocTab")
    ext.set("author", "NaxzyAuxxy")
    ext.set("credits", "Based on TAB by NEZNAMY (Apache-2.0)")
}

val platformPaths = setOf(
    ":bukkit",
    ":bukkit:paper_1_20_5",
    ":bukkit:paper_1_21_2",
    ":bukkit:paper_1_21_4",
    ":bukkit:paper_1_21_9",
    ":bukkit:paper_1_21_11",
    ":bukkit:paper_26_2",
    ":bukkit:paper_chat",
    ":bukkit:v1_7_R4",
    ":bukkit:v1_8_R3",
    ":bukkit:v1_12_R1",
    ":bukkit:v1_16_R3",
    ":bukkit:v1_17_R1",
    ":bukkit:v1_18_R2",
    ":bukkit:v1_19_R1",
    ":bukkit:v1_19_R2",
    ":bukkit:v1_19_R3",
    ":bukkit:v1_20_R1",
    ":bukkit:v1_20_R2",
    ":bukkit:v1_20_R3",
    ":bukkit:v1_20_R4",
    ":bukkit:v1_21_R1",
    ":bukkit:v1_21_R2",
    ":bukkit:v1_21_R3",
    ":bukkit:v1_21_R4",
    ":bukkit:v1_21_R5",
    ":bukkit:v1_21_R6",
    ":bukkit:v1_21_R7",
    ":bukkit:v26_1",
    ":bukkit:v26_2",
    ":bungeecord",
    ":velocity",
    ":fabric",
    ":fand",
    ":neoforge"
//    ":forge"
)

val specialPaths = setOf(
    ":api",
    ":shared"
)

subprojects {
    when (path) {
        in platformPaths -> plugins.apply("havoctab.platform-conventions")
        in specialPaths -> plugins.apply("havoctab.standard-conventions")
        else -> plugins.apply("havoctab.base-conventions")
    }
}
