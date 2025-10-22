import iconfont.GenerateIconFontTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "az.theternal.cmplayground"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "az.theternal.cmplayground"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

val generateIconFont = tasks.register<GenerateIconFontTask>("generateIconFont") {
    inputDir.set(layout.projectDirectory.dir("icon-font/svg"))
    fontOutputFile.set(layout.projectDirectory.file("src/commonMain/composeResources/font/icons.otf"))
    kotlinOutputFile.set(layout.buildDirectory.file("generated/iconFont/IconData.kt"))
    fontName.set("CM Icons")
    fontStyle.set("Regular")
    kotlinPackage.set("az.theternal.cmplayground")
    enumClassName.set("IconData")
}

kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(generateIconFont.map { it.kotlinOutputFile.get().asFile.parentFile })
}

tasks.withType<AbstractKotlinCompile<*>>().configureEach {
    dependsOn(generateIconFont)
}

tasks.matching { task ->
    task.name.startsWith("prepareComposeResourcesTaskFor") ||
        task.name.startsWith("copyNonXmlValueResourcesFor")
}.configureEach {
    dependsOn(generateIconFont)
}
