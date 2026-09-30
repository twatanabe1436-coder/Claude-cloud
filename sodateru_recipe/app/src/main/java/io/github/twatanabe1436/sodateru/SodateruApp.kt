package io.github.twatanabe1436.sodateru

import android.app.Application
import android.content.Context
import io.github.twatanabe1436.sodateru.data.BackupManager
import io.github.twatanabe1436.sodateru.data.DraftHolder
import io.github.twatanabe1436.sodateru.data.LocalDatabase
import io.github.twatanabe1436.sodateru.data.PhotoStore
import io.github.twatanabe1436.sodateru.data.Repository
import io.github.twatanabe1436.sodateru.data.SecretStore
import io.github.twatanabe1436.sodateru.data.SettingsStore
import io.github.twatanabe1436.sodateru.ocr.TextReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SodateruApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** アプリ全体で 1 つずつ使うもの。 */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val photos = PhotoStore(context)
    val repository = Repository(LocalDatabase(context), photos, scope)
    val settings = SettingsStore(context)
    val secrets = SecretStore(context)
    val backup = BackupManager(context, repository, photos, settings)
    val textReader = TextReader(context, photos, settings, secrets)
    val drafts = DraftHolder()
}
