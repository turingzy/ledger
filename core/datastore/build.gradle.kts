plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "cn.turingzy.ledger.core.datastore"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

