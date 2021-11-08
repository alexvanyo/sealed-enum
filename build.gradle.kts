// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.kotlin.jvm)
    jacoco
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinxBinaryCompatibilityValidator)
    `maven-publish`
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin<org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper>()
        plugin<JacocoPlugin>()
        plugin<io.gitlab.arturbosch.detekt.DetektPlugin>()
        plugin<org.jetbrains.dokka.gradle.DokkaPlugin>()
    }

    dependencies {
        detektPlugins(rootProject.libs.detekt.formatting)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        explicitApi()
    }

    tasks {
        compileKotlin {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
                allWarningsAsErrors = true
                freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
            }
        }

        compileTestKotlin {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
                allWarningsAsErrors = true
                freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
            }
        }

        test {
            useJUnitPlatform()
        }

        jacoco {
            toolVersion = rootProject.libs.versions.jacoco.get()
        }

        jacocoTestReport {
            dependsOn(test)

            reports {
                html.required.set(true)
                xml.required.set(true)
            }
        }

        withType<io.gitlab.arturbosch.detekt.Detekt> {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }

        check {
            dependsOn("detektMain")
        }

        dokkaHtml {
            outputDirectory.set(javadoc.get().destinationDir)
        }

        plugins.withType(MavenPublishPlugin::class) {
            val sourcesJar by creating(Jar::class) {
                archiveClassifier.set("sources")
                from(sourceSets.main.get().allSource)
            }

            val javadocJar by creating(Jar::class) {
                archiveClassifier.set("javadoc")
                from(dokkaHtml)
            }

            publishing {
                publications {
                    create<MavenPublication>("default") {
                        from(this@subprojects.components["java"])
                        artifact(sourcesJar)
                        artifact(javadocJar)

                        pom {
                            name.set(project.name)
                            description.set("Obsoleting enums with sealed classes of objects")
                            url.set("https://github.com/livefront/sealed-enum")

                            licenses {
                                license {
                                    name.set("The Apache Software License, Version 2.0")
                                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                    distribution.set("repo")
                                }
                            }

                            developers {
                                developer {
                                    id.set("alexvanyo")
                                    name.set("Alex Vanyo")
                                    organization.set("Livefront")
                                    organizationUrl.set("https://www.livefront.com")
                                }
                            }

                            scm {
                                url.set("https://github.com/livefront/sealed-enum")
                                connection.set("scm:git:git://github.com/livefront/sealed-enum.git")
                                developerConnection.set("scm:git:git://github.com/livefront/sealed-enum.git")
                            }
                        }
                    }
                }
            }
        }
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

apiValidation {
    ignoredPackages.add("com.livefront.sealedenum.internal")
}

tasks {
    jacocoTestReport {
        dependsOn(subprojects.map { it.tasks.jacocoTestReport })
        sourceSets(*subprojects.map { it.sourceSets.main.get() }.toTypedArray())

        executionData.setFrom(
            fileTree(rootDir) {
                include("**/build/jacoco/test.exec")
            }
        )

        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }
}
