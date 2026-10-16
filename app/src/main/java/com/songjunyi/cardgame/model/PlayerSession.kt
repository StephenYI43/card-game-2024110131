package com.songjunyi.cardgame.model

data class PlayerProfile(
    val name: String,
    val studentId: String,
    val gender: Int = 0,
    val phone: String = "",
    val email: String = "",
    val hobbies: List<Int> = emptyList(),
    val bestScore: Int = 0
)

/** Memory-only session: editing profile information never writes a file. */
class PlayerSession(initial: PlayerProfile) {
    var profile = initial
        private set
    fun updateProfile(updated: PlayerProfile) {
        profile = updated.copy(bestScore = profile.bestScore, hobbies = updated.hobbies.toList())
    }
    fun recordScore(score: Int) {
        if (score > profile.bestScore) profile = profile.copy(bestScore = score)
    }
}
