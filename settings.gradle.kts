pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "android_bysj_demo"
//include(":app")
// 引入模块
include(":app", ":mediasoup-client")
// 指定mediasoup-client模块路径（与app同级）
project(":mediasoup-client").projectDir = file("mediasoup-client")