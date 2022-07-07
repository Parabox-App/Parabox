package com.ojhdtapp.parabox.di

import android.app.Application
import androidx.room.Room
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.Converters
import com.ojhdtapp.parabox.data.repository.MainRepositoryImpl
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.use_case.GetContacts
import com.ojhdtapp.parabox.domain.use_case.GetGroupInfoPack
import com.ojhdtapp.parabox.domain.use_case.HandleNewMessage
import com.ojhdtapp.parabox.domain.util.GsonParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    fun provideMainRepository(database: AppDatabase): MainRepository =
        MainRepositoryImpl(database)

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
}