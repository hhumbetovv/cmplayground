plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "az.theternal.mvi.test"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt())
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    iosArm64()
    iosSimulatorArm64()
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":mvi"))
            api(libs.kotlin.test)
            api(libs.kotlinx.coroutines.test)
        }
    }
}
