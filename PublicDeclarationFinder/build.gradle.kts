plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-compiler")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}