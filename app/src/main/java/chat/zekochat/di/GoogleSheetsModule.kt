package chat.zekochat.di

import chat.zekochat.api.routes.googlesheets.ServerDataRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing Google Sheets related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object GoogleSheetsModule {

    /**
     * Provides a singleton instance of ServerDataRepository
     * @return ServerDataRepository instance
     */
    @Provides
    @Singleton
    fun provideServerDataRepository(): ServerDataRepository {
        return ServerDataRepository()
    }
} 