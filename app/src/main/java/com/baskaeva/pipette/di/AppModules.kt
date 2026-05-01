package com.baskaeva.pipette.di

import android.content.Context
import androidx.room.Room
import com.baskaeva.pipette.data.FavouritesRepositoryImpl
import com.baskaeva.pipette.data.PaletteRepositoryImpl
import com.baskaeva.pipette.data.local.FavouriteColorDao
import com.baskaeva.pipette.data.local.MIGRATION_1_2
import com.baskaeva.pipette.data.local.PipetteDatabase
import com.baskaeva.pipette.domain.FavouritesRepository
import com.baskaeva.pipette.domain.PaletteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PipetteDatabase =
        Room.databaseBuilder(
            context,
            PipetteDatabase::class.java,
            "pipette_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideFavouriteColorDao(db: PipetteDatabase): FavouriteColorDao =
        db.favouriteColorDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPaletteRepository(impl: PaletteRepositoryImpl): PaletteRepository

    @Binds
    @Singleton
    abstract fun bindFavouritesRepository(impl: FavouritesRepositoryImpl): FavouritesRepository
}