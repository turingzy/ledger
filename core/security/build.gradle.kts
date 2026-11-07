plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "cn.turingzy.ledger.core.security"
}

dependencies {
    implementation(projects.core.common)

    testImplementation(libs.junit)
}

