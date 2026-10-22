plugins {
    base
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    group = "io.github.sumeetg13"
    // Release builds pass -PreleaseVersion=<x.y.z> (the publish workflow takes it from the git tag).
    // Everything else builds the next snapshot.
    version = providers.gradleProperty("releaseVersion").getOrElse("0.2.0-SNAPSHOT")

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withType<JavaPlugin> {
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(17)
            options.encoding = "UTF-8"
            options.compilerArgs.add("-Xlint:all,-processing,-serial")
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("failed")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
            (options as StandardJavadocDocletOptions).addStringOption("Xmaxwarns", "1")
        }
    }
}
