plugins {
    java
    alias(libs.plugins.spring.boot)
}

description = "Sample Spring Boot app using validate4j"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":validate4j-spring-boot-starter"))
    implementation(libs.spring.boot.starter.webmvc)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testRuntimeOnly(libs.junit.launcher)
}

// Serve resources straight from src/main/resources so validate4j.hot-reload sees edits on save.
tasks.bootRun {
    sourceResources(sourceSets.main.get())
}
