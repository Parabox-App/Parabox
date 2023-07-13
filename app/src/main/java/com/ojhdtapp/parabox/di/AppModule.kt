package com.ojhdtapp.parabox.di

import MainRepositoryImpl
import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.Converters
import com.ojhdtapp.parabox.data.repository.ChatRepositoryImpl
import com.ojhdtapp.parabox.data.repository.ContactRepositoryImpl
import com.ojhdtapp.parabox.data.repository.MessageRepositoryImpl
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import com.ojhdtapp.parabox.domain.repository.ContactRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.repository.MessageRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.GetContact
import com.ojhdtapp.parabox.domain.use_case.GetMessage
import com.ojhdtapp.parabox.domain.use_case.Query
import com.ojhdtapp.parabox.domain.util.GsonParser
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

    @Provides
    @Singleton
    fun provideExtensionManager(
        @ApplicationContext applicationContext: Context,
    ): ExtensionManager = ExtensionManager(applicationContext)

    @Provides
    @Singleton
    fun provideMainRepository(
        @ApplicationContext applicationContext: Context,
        database: AppDatabase
    ): MainRepository = MainRepositoryImpl(context = applicationContext, db = database)

    @Provides
    @Singleton
    fun provideContactRepository(
        @ApplicationContext applicationContext: Context,
        database: AppDatabase
    ): ContactRepository = ContactRepositoryImpl(context = applicationContext, db = database)

    @Provides
    @Singleton
    fun provideChatRepository(
        @ApplicationContext applicationContext: Context,
        database: AppDatabase
    ): ChatRepository = ChatRepositoryImpl(context = applicationContext, db = database)

    @Provides
    @Singleton
    fun provideMessageRepository(
        @ApplicationContext applicationContext: Context,
        database: AppDatabase
    ): MessageRepository = MessageRepositoryImpl(context = applicationContext, db = database)

    @Provides
    @Singleton
    fun provideGetChatUseCase(
        chatRepository: ChatRepository
    ): GetChat = GetChat(chatRepository)

    @Provides
    @Singleton
    fun provideGetMessageUseCase(
        messageRepository: MessageRepository
    ): GetMessage = GetMessage(messageRepository)

    @Provides
    @Singleton
    fun provideGetContactUseCase(
        contactRepository: ContactRepository
    ): GetContact = GetContact(contactRepository)

    @Provides
    @Singleton
    fun provideQueryUseCase(
        messageRepository: MessageRepository,
        contactRepository: ContactRepository,
        chatRepository: ChatRepository
    ): Query = Query(
        messageRepository, contactRepository, chatRepository
    )
}