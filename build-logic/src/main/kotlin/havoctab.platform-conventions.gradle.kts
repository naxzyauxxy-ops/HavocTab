plugins {
    id("havoctab.standard-conventions")
    id("com.gradleup.shadow")
}

tasks.withType<Zip>().configureEach {
    isZip64 = true
}

tasks {
    shadowJar {
        archiveFileName.set("HavocTab-${project.name}-${project.version}.jar")
        relocate("org.bstats", "dev.havoc.havoctab.libs.org.bstats")
        relocate("org.json.simple", "dev.havoc.havoctab.libs.org.json.simple")
        relocate("net.kyori.event", "dev.havoc.havoctab.libs.net.kyori.event")
        relocate("me.neznamy.yamlassist", "dev.havoc.havoctab.libs.me.neznamy.yamlassist")
        relocate("org.yaml.snakeyaml", "dev.havoc.havoctab.libs.org.yaml.snakeyaml")
        relocate("redis.clients.jedis", "dev.havoc.havoctab.libs.redis.clients.jedis")
        relocate("org.apache.commons.pool2", "dev.havoc.havoctab.libs.org.apache.commons.pool2")
        relocate("org.json", "dev.havoc.havoctab.libs.org.json")
        relocate("com.rabbitmq", "dev.havoc.havoctab.libs.com.rabbitmq")
        relocate("com.saicone.delivery4j", "dev.havoc.havoctab.libs.com.saicone.delivery4j")
    }
}
