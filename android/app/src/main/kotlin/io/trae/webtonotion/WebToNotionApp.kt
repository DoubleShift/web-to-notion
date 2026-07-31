package io.trae.webtonotion

import android.app.Application
import io.trae.webtonotion.data.repository.NoteRepository

class WebToNotionApp : Application() {
    val repository by lazy { NoteRepository.getInstance(this) }
}
