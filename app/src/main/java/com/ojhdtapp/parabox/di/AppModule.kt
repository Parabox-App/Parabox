package com.ojhdtapp.parabox.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ojhdtapp.parabox.core.util.DownloadUtil
import com.ojhdtapp.parabox.core.util.DownloadUtilService
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.core.util.OnedriveUtil
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.Converters
import com.ojhdtapp.parabox.data.remote.dto.ReceiveMessageDtoJsonDeserializer
import com.ojhdtapp.parabox.data.remote.dto.SendMessageDtoJsonDeserializer
import com.ojhdtapp.parabox.data.remote.dto.onedrive.MsalApi
import com.ojhdtapp.parabox.data.remote.dto.server.content.Content
import com.ojhdtapp.parabox.data.remote.dto.server.content.ContentTypeAdapter
import com.ojhdtapp.parabox.data.repository.MainRepositoryImpl
import com.ojhdtapp.parabox.domain.fcm.FcmApiHelper
import com.ojhdtapp.parabox.domain.fcm.FcmService
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.use_case.*
import com.ojhdtapp.parabox.domain.util.GsonParser
import com.ojhdtapp.parabox.domain.worker.UploadFileWorker
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
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
    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase =
        Room.databaseBuilder(
            app, AppDatabase::class.java,
            "main_db"
        )
            .addTypeConverter(Converters(GsonParser()))
            .build()

    @Provides
    @Singleton
    fun provideMainRepository(
        database: AppDatabase,
        @ApplicationContext applicationContext: Context,
        notificationUtil: NotificationUtil,
        fcmApiHelper: FcmApiHelper
    ): MainRepository =
        MainRepositoryImpl(database, applicationContext, notificationUtil, fcmApiHelper)

    @Provides
    @Singleton
    fun provideNotificationUtil(
        @ApplicationContext applicationContext: Context,
        database: AppDatabase,
    ): NotificationUtil =
        NotificationUtil(applicationContext, database)

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
    fun provideFcmService(retrofit: Retrofit): FcmService =
        retrofit.create(FcmService::class.java)

    @Provides
    @Singleton
    fun provideFcmApiHelper(
        fcmService: FcmService,
        @ApplicationContext applicationContext: Context
    ) = FcmApiHelper(
        applicationContext,
        fcmService,
    )

    @Provides
    @Singleton
    fun provideDownloadUtilService(retrofit: Retrofit): DownloadUtilService =
        retrofit.create(DownloadUtilService::class.java)

    @Provides
    @Singleton
    fun provideMsalService(): MsalApi{
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(OnedriveUtil.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MsalApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOnedriveUtil(
        @ApplicationContext applicationContext: Context,
        msalApi: MsalApi,
    ) = OnedriveUtil(
        applicationContext,
        msalApi,
    )

    @Provides
    @Singleton
    fun provideDownloadUtil(
        downloadUtilService: DownloadUtilService,
        @ApplicationContext applicationContext: Context
    ) = DownloadUtil(
        applicationContext,
        downloadUtilService,
    )

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .registerTypeAdapter(
            ReceiveMessageDto::class.java,
            ReceiveMessageDtoJsonDeserializer()
        )
        .registerTypeAdapter(
            SendMessageDto::class.java,
            SendMessageDtoJsonDeserializer()
        )
        .registerTypeAdapter(
            Content::class.java,
            ContentTypeAdapter()
        )
        .create()

    @Provides
    @Singleton
    fun provideHandleNewMessageUseCase(repository: MainRepository): HandleNewMessage {
        return HandleNewMessage(repository)
    }

    @Provides
    @Singleton
    fun provideGetContactListUseCase(repository: MainRepository): GetContacts {
        return GetContacts(repository)
    }

    @Provides
    @Singleton
    fun provideGetGroupInfoPackUseCase(repository: MainRepository): GetGroupInfoPack {
        return GetGroupInfoPack(repository)
    }

    @Provides
    @Singleton
    fun provideGroupNewContactUseCase(repository: MainRepository): GroupNewContact {
        return GroupNewContact(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateContactUseCase(repository: MainRepository): UpdateContact {
        return UpdateContact(repository)
    }

    @Provides
    @Singleton
    fun provideTagControlUseCase(repository: MainRepository): TagControl {
        return TagControl(repository)
    }

    @Provides
    @Singleton
    fun provideGetArchivedContactsUseCase(repository: MainRepository): GetArchivedContacts {
        return GetArchivedContacts(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateMessageUseCase(repository: MainRepository): UpdateMessage {
        return UpdateMessage(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteGroupedContactUseCase(repository: MainRepository): DeleteGroupedContact {
        return DeleteGroupedContact(repository)
    }

    @Provides
    @Singleton
    fun provideDeleteMessageUseCase(repository: MainRepository): DeleteMessage {
        return DeleteMessage(repository)
    }

    @Provides
    @Singleton
    fun provideGetFilesUseCase(repository: MainRepository): GetFiles {
        return GetFiles(repository)
    }

    @Provides
    @Singleton
    fun provideQueryContactAndMessageUseCase(repository: MainRepository): QueryContactAndMessage {
        return QueryContactAndMessage(repository)
    }

    @Provides
    @Singleton
    fun provideGetUriFromCloudResourceInfoUseCase(
        @ApplicationContext applicationContext: Context,
        downloadUtil: DownloadUtil
    ): GetUriFromCloudResourceInfo {
        return GetUriFromCloudResourceInfo(context = applicationContext, downloadUtil = downloadUtil)
    }
}