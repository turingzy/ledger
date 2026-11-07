plugins {
    alias(libs.plugins.ledger.android.application)
    alias(libs.plugins.ledger.android.compose)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "cn.turingzy.ledger"

    defaultConfig {
        applicationId = "cn.turingzy.ledger"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    // 依赖所有 Feature 特性模块
    implementation(projects.feature.transaction)
    implementation(projects.feature.account)
    implementation(projects.feature.statistics)
    implementation(projects.feature.category)
    implementation(projects.feature.importer)
    implementation(projects.feature.backup)
    implementation(projects.feature.settings)

    // 核心支撑层
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.navigation)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.sync)

    // AndroidX 核心与导航
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
