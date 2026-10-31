plugins {
    id("java-library")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.helpch.at/releases/")
    maven("https://jitpack.io")

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/networkminesky/AnarchyEconomy")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR") ?: "SEU_USUARIO"
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN") ?: "ghp_SEU_TOKEN"
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("org.mongodb:mongodb-driver-sync:5.6.0")
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("com.github.networkminesky:AnarchyCore:1.1.0")
    compileOnly("com.github.networkminesky:AnarchyVanish:1.0.0")
    compileOnly("anarchy.minesky:anarchyeconomy-api:1.0.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
