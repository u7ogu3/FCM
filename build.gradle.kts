plugins {
    application
    java
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
    options.encoding = "UTF-8"
}

application {
    // ACM.java ist die Main-Klasse im Repo-Root
    mainClass.set("com.acm.main.ACM")
}

repositories { mavenCentral() }

dependencies {
    // Logging (optional, aber sinnvoll)
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.7")

    // JNA: Alternative zum mitgelieferten syshook.dll (siehe Abschnitt 2)
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
}

tasks.jar {
    manifest { attributes["Main-Class"] = application.mainClass.get() }
}
