package com.gptimage.playground.di

import android.content.Context
import androidx.room.Room
import com.gptimage.playground.data.local.AppDatabase
import com.gptimage.playground.data.local.ImageStorage
import com.gptimage.playground.data.remote.OpenAICompatibleClient
import com.gptimage.playground.data.repository.ConfigRepository
import com.gptimage.playground.data.repository.HistoryRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Manual dependency-injection container held by [com.gptimage.playground.PlaygroundApplication].
 * Keeps the app free of Hilt/KSP-DI complexity while still providing singletons
 * with a lifecycle tied to the process.
 */
class AppContainer(private val context: Context) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 120_000
            connectTimeoutMillis = 20_000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
        }
    }

    private val database: AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    val imageStorage: ImageStorage = ImageStorage(context)

    val configRepository: ConfigRepository = ConfigRepository(context, json)

    val historyRepository: HistoryRepository = HistoryRepository(
        dao = database.historyDao(),
        storage = imageStorage,
        json = json
    )

    val openAIClient: OpenAICompatibleClient = OpenAICompatibleClient(httpClient, json)
}
