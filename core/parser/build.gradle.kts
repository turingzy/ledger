plugins {
    alias(libs.plugins.ledger.jvm.library)
}

dependencies {
    implementation(projects.core.model)
    testImplementation(libs.junit)
}

