plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvmToolchain(21)

    // PlatformSeams 刻意使用 expect/actual class（Beta 特性）：压掉每次编译刷出的两条 Beta 提示。
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "top.yukonga.scripta.sandbox.shared"
        compileSdk {
            version = release(37) {
                minorApiLevel = 2
            }
        }
        minSdk = 24
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(projects.editor)
            api(libs.compose.foundation)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}
