plugins {
    `java-library`
    `maven-publish`
}

group = "eu.sekunity"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.spongepowered.org/maven/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.2")
}

publishing {
    publications {
        create<MavenPublication>("sekunityApi") {
            from(components["java"])
            artifactId = "sekunity-api-v2"
        }
    }

    repositories {
        maven {
            name = "sekunityPrivate"
            url = uri("https://repo.sekunity.eu/private")
            credentials {
                username = (findProperty("sekunityUsername") as String?)
                password = (findProperty("sekunityPassword") as String?)
            }
        }
    }
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

