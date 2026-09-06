import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    id("jacoco")
}

android {
    namespace = "com.tasirin.browser"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tasirin.browser"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
}

// --- JaCoCo coverage ---
tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension>("jacoco") {
        isIncludeNoLocationClasses = true
        excludes = (excludes ?: emptyList()) + "jdk.internal.*"
    }
}

val jacocoExecData = layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")
val jacocoClassDirs = files(
    layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
    layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    reports {
        xml.required.set(true)
        html.required.set(false)
        csv.required.set(false)
    }
    classDirectories.setFrom(jacocoClassDirs)
    executionData.setFrom(jacocoExecData)
    sourceDirectories.setFrom(files("src/main/java"))
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("testDebugUnitTest")
    group = "verification"
    executionData.setFrom(jacocoExecData)
    classDirectories.setFrom(jacocoClassDirs)
    sourceDirectories.setFrom(files("src/main/java"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.0".toBigDecimal()
            }
        }
    }
}
