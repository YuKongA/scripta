plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

@Suppress("UnstableApiUsage")
android {
    namespace = "top.yukonga.scripta"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }
    defaultConfig {
        applicationId = "top.yukonga.scripta"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release {
            optimization.enable = true
            vcsInfo.include = false
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packaging {
        jniLibs {
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes
            .add("**")
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.sandbox.shared)
    implementation(libs.androidx.activity.compose)
}
