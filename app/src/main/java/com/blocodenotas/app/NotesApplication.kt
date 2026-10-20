package com.blocodenotas.app

import android.app.Application
import com.blocodenotas.app.data.AppSettings
import com.blocodenotas.app.data.NotesRepository

class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}

/** Acesso simples aos objetos compartilhados do app. */
object Graph {
    lateinit var repo: NotesRepository
        private set
    lateinit var settings: AppSettings
        private set

    /** Evita bloquear o app quando ele sai de cena só para abrir o seletor de arquivos. */
    var skipNextLock = false

    fun init(app: Application) {
        repo = NotesRepository(app)
        settings = AppSettings(app)
    }
}
