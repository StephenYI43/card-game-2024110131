package com.songjunyi.cardgame
import android.content.Context
import com.songjunyi.cardgame.model.*
import java.io.*

data class Archive(val profile: PlayerProfile, val pair: GameSnapshot, val triple: GameSnapshot) : Serializable {
    private companion object { const val serialVersionUID = 1L }
}

class ArchiveStore(private val context: Context) {
    private val filename = "fragment-state-v1.bin"
    fun read(): Archive? = try {
        ObjectInputStream(context.openFileInput(filename)).use {
            (it.readObject() as Archive).also { state ->
                require(state.pair.groupSize == 2 && state.triple.groupSize == 3)
                CardMatchingGame.restore(state.pair)
                CardMatchingGame.restore(state.triple)
            }
        }
    } catch (_: Exception) { null }

    fun write(state: Archive): Boolean = try {
        val temporary = filename + ".tmp"
        context.openFileOutput(temporary, Context.MODE_PRIVATE).use { file ->
            ObjectOutputStream(file).use {
                it.writeObject(state)
                it.flush()
                file.fd.sync()
            }
        }
        context.getFileStreamPath(temporary).renameTo(context.getFileStreamPath(filename))
    } catch (_: IOException) { false }
}
