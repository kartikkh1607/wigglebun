import com.android.build.api.variant.HostTestBuilder

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.kartikkhandelwal.wigglebun"
    // Compile against 37 because current Compose / Navigation / core-ktx require it (minCompileSdk = 37).
    // targetSdk stays 36, which is what Google Play requires.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kartikkhandelwal.wigglebun"
        minSdk = 26
        targetSdk = 36
        // Play rejects an upload whose versionCode it has seen before, on any track. Add 1 for every
        // AAB uploaded (closed test or production); never reuse or lower it. versionName is what
        // people see; closed-test builds add "-closedtest" to it (below).
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Closing the paywall leaves the app locked (CLAUDE.md §5.5). Only the closedTest build
        // turns this off; ReleaseBuildTest fails CI if release ever does.
        buildConfigField("boolean", "PAYWALL_LOCKS", "true")
    }

    // The upload key never goes into git. Put these four in ~/.gradle/gradle.properties (or pass
    // -P...); without them the release builds come out unsigned, which is what CI builds.
    val uploadStoreFile = providers.gradleProperty("WIGGLEBUN_UPLOAD_STORE_FILE").orNull
    val uploadKey = uploadStoreFile?.let {
        signingConfigs.create("upload") {
            storeFile = file(it)
            storePassword = providers.gradleProperty("WIGGLEBUN_UPLOAD_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("WIGGLEBUN_UPLOAD_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("WIGGLEBUN_UPLOAD_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            signingConfig = uploadKey
        }
        // The Play closed test (CLAUDE.md §12): the release build, except that closing the paywall
        // doesn't lock, because Play's test subscriptions lapse within hours. Never promote a
        // closedTest release to production; upload a release build instead.
        create("closedTest") {
            initWith(getByName("release"))
            versionNameSuffix = "-closedtest"
            matchingFallbacks += "release"
            buildConfigField("boolean", "PAYWALL_LOCKS", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // BuildConfig.DEBUG keeps diagnostic logging out of release builds.
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Robolectric needs merged resources (fonts, strings) for screenshot tests.
            isIncludeAndroidResources = true
            all {
                // Robolectric pokes at FileDescriptor internals, which newer JDKs lock down.
                it.jvmArgs(
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--enable-native-access=ALL-UNNAMED",
                )
                // Robolectric downloads its android-all jars from Maven Central at test time;
                // use the same mirror as settings.gradle.kts so CI doesn't hit Central's 429s.
                it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
            }
        }
    }
}

androidComponents {
    // AGP 9 only makes unit-test tasks for debug. Release needs them too, for ReleaseBuildTest
    // (src/testRelease), which CI runs on its own: testReleaseUnitTest --tests '*.ReleaseBuildTest'.
    beforeVariants(selector().withBuildType("release")) { variant ->
        variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}

roborazzi {
    // Reference screenshots are checked in, so verifyRoborazziDebug fails on unintended UI changes.
    // Compare/diff images still go to build/outputs/roborazzi.
    outputDir.set(file("src/test/screenshots"))
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // ProcessLifecycleOwner: sounds only play while the app is in the foreground (feedback/Feel.kt).
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.hilt.android)
    implementation(libs.play.services.base)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    // Activity Recognition Transition API (CLAUDE.md §3).
    implementation(libs.play.services.location)

    // Home-screen widget (CLAUDE.md §8) and its 15-minute refresh.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.work.runtime.ktx)
    debugImplementation(libs.androidx.glance.appwidget.preview)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
