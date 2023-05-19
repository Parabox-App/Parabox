package com.ojhdtapp.parabox.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
//    @Provides
//    @Singleton
//    fun provideAppDatabase(app: Application): AppDatabase =
//        Room.databaseBuilder(
//            app, AppDatabase::class.java,
//            "main_db"
//        )
//            .addTypeConverter(Converters(GsonParser()))
//            .build()

//    @Provides
//    @Singleton
//    fun provideMainRepository(
//        database: AppDatabase,
//        @ApplicationContext applicationContext: Context,
//        notificationUtil: NotificationUtil,
//        fcmApiHelper: FcmApiHelper
//    ): MainRepository =
//        MainRepositoryImpl(database, applicationContext, notificationUtil, fcmApiHelper)
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.google.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .create()
}