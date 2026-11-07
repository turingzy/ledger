plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.room)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "cn.turingzy.ledger.core.database"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
