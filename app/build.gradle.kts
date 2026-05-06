plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.license)
}

android {
    namespace = "de.onemanprojects.klukka"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "de.onemanprojects.klukka"
        minSdk = 31
        targetSdk = 36
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

val mitCompatibleLicenses = setOf(
    "mit",
    "apache 2.0",
    "apache-2.0",
    "the apache software license, version 2.0",
    "the apache license, version 2.0",
    "apache license, version 2.0",
    "bsd 2-clause",
    "bsd 3-clause",
    "bsd-2-clause",
    "bsd-3-clause",
    "isc",
    "unlicense",
    "cc0-1.0",
    "cc0 1.0 universal",
    "boost software license 1.0",
)

tasks.register("checkLicenses") {
    dependsOn("licenseDebugReport")
    val reportFile = layout.buildDirectory.file("reports/licenses/licenseDebugReport.json")
    inputs.file(reportFile)
    doLast {
        val json = reportFile.get().asFile.readText()
        val violations = mutableListOf<String>()
        val entries = json.trim().removePrefix("[").removeSuffix("]").split("},{")
        for (entry in entries) {
            val nameMatch = Regex("\"project\":\"([^\"]+)\"").find(entry)
            val licenseMatches = Regex("\"license\":\"([^\"]+)\"").findAll(entry)
            val project = nameMatch?.groupValues?.get(1) ?: "unknown"
            val licenses = licenseMatches.map { it.groupValues[1] }.toList()
            if (licenses.none { it.lowercase() in mitCompatibleLicenses }) {
                violations.add("$project: ${licenses.ifEmpty { listOf("no license found") }}")
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Non-MIT-compatible licenses detected:\n" + violations.joinToString("\n") { "  - $it" }
            )
        }
        println("License check passed: all ${entries.size} dependencies use MIT-compatible licenses.")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}