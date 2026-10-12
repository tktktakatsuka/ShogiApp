import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// オンライン対戦用の Firebase 設定ファイル(app/google-services.json)がある場合だけ有効にする。
// ない場合もビルドはでき、オンライン対戦メニューが無効になる。
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// 広告(AdMob)の ID。リリース版では gradle.properties か local.properties の
// admobAppId / admobBannerId を使う。未設定のときと debug 版では Google のテスト用 ID を使う
// (開発中に本物の広告を表示・タップすると AdMob の規約違反になるため)。
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestBannerId = "ca-app-pub-3940256099942544/9214589741"
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun admobId(name: String, testId: String): String =
    (localProperties.getProperty(name) ?: providers.gradleProperty(name).orNull)?.takeIf { it.isNotBlank() } ?: testId

android {
    namespace = "com.tktkcompany.shogi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tktkcompany.shogi"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = admobTestAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobTestBannerId\"")
        }
        release {
            manifestPlaceholders["admobAppId"] = admobId("admobAppId", admobTestAppId)
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${admobId("admobBannerId", admobTestBannerId)}\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        // 検討用エンジン(jniLibs の libyaneuraou.so)は別プロセスとして実行するので、
        // インストール時に APK から取り出してもらう必要がある。
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.ads)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
