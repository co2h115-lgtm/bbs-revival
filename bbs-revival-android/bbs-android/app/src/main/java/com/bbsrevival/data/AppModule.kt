package com.bbsrevival.data

import android.content.Context
import com.bbsrevival.data.api.BbsApiClient
import com.bbsrevival.data.api.SocketManager
import com.bbsrevival.data.api.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideContext(@ApplicationContext ctx: Context): Context = ctx

    @Provides @Singleton
    fun provideTokenStore(@ApplicationContext ctx: Context) = TokenStore(ctx)

    @Provides @Singleton
    fun provideApiClient(tokenStore: TokenStore) = BbsApiClient(tokenStore)

    @Provides @Singleton
    fun provideSocketManager(tokenStore: TokenStore) = SocketManager(tokenStore)

    @Provides @Singleton
    fun provideNotificationHelper(@ApplicationContext ctx: Context) = NotificationHelper(ctx)
}
