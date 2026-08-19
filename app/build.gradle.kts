plugins {
    // AGP 9 da co san ho tro Kotlin — them `org.jetbrains.kotlin.android` vao la
    // build do ngay ("plugin is no longer required since AGP 9.0").
    id("com.android.application")
}

/*
 * Ghi chu — app doc lap, cai duoc len may Android bat ky.
 *
 * Ban truoc la ban MOD tiem thang vao APK he thong cua Xiaomi. Cach do chet vi
 * mot loi khong lien quan gi den code: APK he thong lay tu may khac, chay khong
 * co dac quyen tren HyperOS 3, do vo trong ICU/Scudo sau ~9 giay moi lan mo.
 * Da chung minh bang phep chia doi: ban chi doi ten goi, KHONG tiem gi, van chet
 * y het. Nen bo han huong do.
 *
 * KHONG KEO ANDROIDX VAO — co y:
 *   * 4.888 dong Kotlin mang tu ban mod sang deu dung API khung san co, dung UI
 *     bang code. Keo AndroidX vao la de hai loi viet ma xen nhau.
 *   * APK giu duoi ~3 MB, khong ProGuard, khong lo xung dot theme.
 *   * Moi thu can (ListView, AlertDialog, AppWidget, camera2) khung deu co.
 */
android {
    namespace = "com.ghichu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ghichu"
        // Android 11. Code mang sang dung thang WindowInsets.Type (API 30);
        // ha thap hon phai rai if/else khap noi ma doi lai vai phan tram may.
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // Java THUAN, khong tai nguyen, khong goi API Android — dung de quet QR.
    implementation("com.google.zxing:core:3.5.3")

    // Kiem thu chay tren JVM, khong can dien thoai. Phan loi (dung loi nhac,
    // doc JSON Gemini, doi markup, lich lap lai) co y viet thuan Kotlin de
    // kiem duoc o day.
    testImplementation("junit:junit:4.13.2")
    // org.json trong android.jar chi la vo rong; keo ban that vao rieng cho test.
    testImplementation("org.json:json:20250107")
}
