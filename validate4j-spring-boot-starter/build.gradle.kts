plugins {
    `java-library`
    alias(libs.plugins.maven.publish)
}

description = "validate4j Spring Boot starter: auto-configuration, @ValidateWith and ProblemDetail error handling"

dependencies {
    api(project(":validate4j-core"))
    api(platform(libs.spring.boot.bom))
    api(libs.spring.boot.starter)
    api(libs.spring.boot.autoconfigure)
    compileOnly(libs.spring.boot.starter.webmvc)
    annotationProcessor(platform(libs.spring.boot.bom))
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(libs.spring.boot.starter.webmvc)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testRuntimeOnly(libs.junit.launcher)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "validate4j-spring-boot-starter", version.toString())

    pom {
        name.set("validate4j-spring-boot-starter")
        description.set(project.description)
        url.set("https://github.com/sumeetg13/validate4j")
        inceptionYear.set("2026")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("sumeetg13")
                name.set("Sumeet Gupta")
                url.set("https://github.com/sumeetg13")
            }
        }
        scm {
            url.set("https://github.com/sumeetg13/validate4j")
            connection.set("scm:git:git://github.com/sumeetg13/validate4j.git")
            developerConnection.set("scm:git:ssh://git@github.com/sumeetg13/validate4j.git")
        }
        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/sumeetg13/validate4j/issues")
        }
    }
}
