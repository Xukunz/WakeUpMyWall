@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        // kotlinx-datetime 0.8.0 的 Instant 是 kotlin.time.Instant 的类型别名，需要显式 opt-in。
        optIn.add("kotlin.time.ExperimentalTime")
    }

    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    // 仅用于 Compose UI 测试与设计预览，不产出桌面产品形态
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(compose.uiTooling)
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.uiTest)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

// 固定的资源包名，避免生成类跟着模块名漂移；资源母版与派生规则见 imgs/wallpaper/README.md。
compose.resources {
    packageOfResClass = "com.xukunz.wakeupmywall.resources"
}

android {
    namespace = "com.xukunz.wakeupmywall"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xukunz.wakeupmywall"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}
