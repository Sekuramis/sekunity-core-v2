plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "eu.sekunity"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.lucko.me/")
    maven("https://repo.sekunity.eu/private")
}

dependencies {
    implementation(project(":Sekunity-API-v2"))
    implementation(project(":Sekunity-Paper-Core"))

    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.5")

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}