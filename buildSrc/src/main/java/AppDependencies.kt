
import org.gradle.api.artifacts.dsl.DependencyHandler

object AppDependencies {
    //std lib
    val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"

    //android
    private val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
    private val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
    private val material = "com.google.android.material:material:${Versions.material}"
    private val lifecycle = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"

    //compose
    private val compose = "androidx.compose.ui:ui:${Versions.compose}"
    private val materialCompose = "androidx.compose.material:material:${Versions.compose}"
    private val activityCompose = "androidx.activity:activity-compose:${Versions.activityCompose}"
    private val composeInsets = "com.google.accompanist:accompanist-insets:${Versions.composeInsets}"
    private val navigationCompose = "androidx.navigation:navigation-compose:${Versions.navigationCompose}"

    //test libs
    private val junit = "junit:junit:${Versions.junit}"
    private val extJUnit = "androidx.test.ext:junit:${Versions.extJunit}"
    private val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espresso}"
    private val composeJUnit = "androidx.compose.ui:ui-test-junit4:${Versions.compose}"

    val appLibraries = arrayListOf(
        coreKtx,
        material,
        appcompat,
        lifecycle,
        kotlinStdLib,
    )

    val jetpackCompose = arrayListOf(
        compose,
        composeInsets,
        materialCompose,
        activityCompose,
        navigationCompose,
    )

    val androidTestLibraries = arrayListOf(
        extJUnit,
        espressoCore,
        composeJUnit
    )

    val testLibraries = arrayListOf(junit)
}

//util functions for adding the different type dependencies from build.gradle file
fun DependencyHandler.kapt(list: List<String>) {
    list.forEach { dependency ->
        add("kapt", dependency)
    }
}

fun DependencyHandler.implementation(list: List<String>) {
    list.forEach { dependency ->
        add("implementation", dependency)
    }
}

fun DependencyHandler.androidTestImplementation(list: List<String>) {
    list.forEach { dependency ->
        add("androidTestImplementation", dependency)
    }
}

fun DependencyHandler.testImplementation(list: List<String>) {
    list.forEach { dependency ->
        add("testImplementation", dependency)
    }
}