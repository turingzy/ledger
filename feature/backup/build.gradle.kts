plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.compose)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "cn.turingzy.ledger.feature.backup"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.core.common)

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

