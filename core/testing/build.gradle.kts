plugins {
    alias(libs.plugins.ledger.jvm.library)
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.turbine)
    implementation(libs.mockk)
    implementation(libs.junit)
}

