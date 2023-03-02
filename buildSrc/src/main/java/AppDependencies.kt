object AppDependencies {
    //std lib
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"

    //android
    private const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
    private const val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
    private const val material = "com.google.android.material:material:${Versions.material}"
    private const val lifecycle = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"

    //network
    private const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"

    //koin
    private const val koin = "io.insert-koin:koin-core:${Versions.koin}"
    private const val koinAndroid = "io.insert-koin:koin-android:${Versions.koin}"
    private const val koinCompose = "io.insert-koin:koin-androidx-compose:${Versions.koin}"

    //compose
    private const val compose = "androidx.compose.ui:ui:${Versions.composeUI}"
    private const val materialCompose = "androidx.compose.material:material:${Versions.composeMaterial}"
    private const val activityCompose =
        "androidx.activity:activity-compose:${Versions.activityCompose}"
    private const val navigationCompose =
        "androidx.navigation:navigation-compose:${Versions.navigationCompose}"
    private const val composeUITooling = "androidx.compose.ui:ui-tooling:${Versions.composeUI}"

    //test libs
    private const val junit = "junit:junit:${Versions.junit}"
    private const val extJUnit = "androidx.test.ext:junit:${Versions.extJunit}"
    private const val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espresso}"
    private const val composeJUnit = "androidx.compose.ui:ui-test-junit4:${Versions.composeUI}"
    private const val koinTest = "io.insert-koin:koin-test:${Versions.koin}"

    val appLibraries = arrayListOf(
        coreKtx,
        material,
        appcompat,
        lifecycle,
        kotlinStdLib,
        retrofit
    )

    val koinLibraries = arrayListOf(
        koin,
        koinAndroid,
        koinCompose
    )

    val jetpackCompose = arrayListOf(
        compose,
        materialCompose,
        activityCompose,
        navigationCompose,
        composeUITooling,
    )

    val androidTestLibraries = arrayListOf(
        extJUnit,
        espressoCore,
        composeJUnit,
    )

    val testLibraries = arrayListOf(
        junit,
        koinTest,
    )
}