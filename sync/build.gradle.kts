plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "cn.turingzy.ledger.sync"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.parser)
    implementation(projects.core.common)

    testImplementation(libs.junit)
}

