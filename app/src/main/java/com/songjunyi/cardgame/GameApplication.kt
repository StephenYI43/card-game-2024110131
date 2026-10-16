package com.songjunyi.cardgame

import android.app.Application
import com.songjunyi.cardgame.model.PlayerProfile
import com.songjunyi.cardgame.model.PlayerSession

class GameApplication : Application() {
    lateinit var session: PlayerSession
        private set
    override fun onCreate() {
        super.onCreate()
        session = PlayerSession(PlayerProfile(getString(R.string.default_name), getString(R.string.default_student)))
    }
}
