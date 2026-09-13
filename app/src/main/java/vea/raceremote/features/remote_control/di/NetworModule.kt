package vea.raceremote.features.remote_control.di

import vea.raceremote.network.RemoteApiProvider
import vea.raceremote.network.RemoteDeviceApi
import vea.raceremote.network.WebSocketApi
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

private const val BASE_URL = "http://192.168.1.100:80/"

val networkModule = module {
    single<Retrofit> {
        Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                    .readTimeout(3, TimeUnit.SECONDS)
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .build()
            )
            .baseUrl(BASE_URL)
            .build()
    }
    single<RemoteDeviceApi> { RemoteApiProvider(get()).provideRemoteDeviceApi() }
    single<WebSocketApi> { WebSocketApi() }
}
