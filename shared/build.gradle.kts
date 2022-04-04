import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
}

version = "1.0"

kotlin {
    android()
    iosX64()
    iosArm64()
    //iosSimulatorArm64() sure all ios dependencies support this target

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
        }
    }
    
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("$buildDir/generated-apis/src/commonMain/kotlin")
            dependencies {
                implementation("io.ktor:ktor-client-core:1.6.5")
                implementation("io.ktor:ktor-client-json:1.6.5")
                implementation("io.ktor:ktor-client-serialization:1.6.5")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            kotlin.srcDir("$buildDir/generated-apis/src/androidMain/kotlin")
        }

        val androidAndroidTestRelease by getting
        val androidTest by getting {
            dependsOn(androidAndroidTestRelease)
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        //val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("$buildDir/generated-apis/src/iosMain/kotlin")
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            //iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        //val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            //iosSimulatorArm64Test.dependsOn(this)
        }
    }
}


fun createOpenApiGenerateTask(yml: String) {
    tasks.register<GenerateTask>("openApiGenerate_$yml") {
        generatorName.set("bright")
        inputSpec.set("$rootDir/api/$yml.json")
        outputDir.set("$buildDir/generated-apis/")
        additionalProperties.put("packageName", "ai.bright.generator.client.$yml")
    }
}

createOpenApiGenerateTask("matter")


android {
    compileSdk = 31
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 23
        targetSdk = 31
    }
}