plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.android_bysj_demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.android_bysj_demo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

//    // 关联CMakeLists.txt
//    externalNativeBuild {
//        cmake {
//            path = file("CMakeLists.txt") // 指向仓库原有CMake文件
//            version = "3.18.1" // 匹配NDK自带CMake版本
//        }
//    }
//
//    // 源码目录配置
//    sourceSets {
//        getByName("main") {
//            java.srcDirs("src/main/java")
//            jniLibs.srcDirs("src/main/jniLibs") // 预编译so库目录
//            jni.srcDirs("src/main/jni") // JNI代码目录
//        }
//    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
//    implementation ("io.github.haiyangwu:mediasoup-client:3.4.0")
    // 核心：依赖本地mediasoup-client模块
    implementation(project(":mediasoup-client"))
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
//    implementation(files("libs\\libwebrtc.jar"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}