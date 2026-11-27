plugins {
    java
}

group = "dev.tavukdoner"
version = "0.2.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.258-stable")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        filesMatching("plugin.yml") { expand("version" to project.version) }
    }
    jar {
        archiveFileName = "TavukDoner-${project.version}.jar"
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }
}
