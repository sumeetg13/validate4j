plugins {
    `java-library`
    alias(libs.plugins.maven.publish)
}

description = "validate4j core: YAML-driven, parallel form validation with no framework dependencies"

dependencies {
    implementation(libs.snakeyaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.archunit)
    testRuntimeOnly(libs.junit.launcher)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "validate4j-core", version.toString())

    pom {
        name.set("validate4j-core")
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
