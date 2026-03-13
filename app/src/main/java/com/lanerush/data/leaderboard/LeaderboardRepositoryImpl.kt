package com.lanerush.data.leaderboard

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lanerush.domain.model.Score
import com.lanerush.domain.model.User
import com.lanerush.domain.repository.LeaderboardRepository
import kotlinx.coroutines.tasks.await

class LeaderboardRepositoryImpl(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LeaderboardRepository {

    private val usersCollection = firestore.collection("users")
    private val scoresCollection = firestore.collection("scores")

    override suspend fun submitScore(score: Score): Result<Unit> = runCatching {
        // We use a flat "scores" collection where the document ID is the UID.
        // This enforces ONE entry per user across all categories.
        val existingDoc = scoresCollection.document(score.uid).get().await()
        val existingScore = existingDoc.toObject(Score::class.java)

        if (existingScore == null) {
            scoresCollection.document(score.uid).set(score).await()
        } else {
            val updates = mutableMapOf<String, Any>()
            // Update individual category records if the new ones are higher
            if (score.score > existingScore.score) updates["score"] = score.score
            if (score.topSpeedReached > existingScore.topSpeedReached) updates["topSpeedReached"] = score.topSpeedReached
            if (score.avgSpeedDuringRace > existingScore.avgSpeedDuringRace) updates["avgSpeedDuringRace"] = score.avgSpeedDuringRace
            
            updates["timestamp"] = System.currentTimeMillis()
            updates["displayName"] = score.displayName
            updates["photoUrl"] = score.photoUrl

            if (updates.isNotEmpty()) {
                scoresCollection.document(score.uid).update(updates).await()
            }
        }
        
        // Also update the User profile for consistency
        updateUserProfile(score)
    }

    private suspend fun updateUserProfile(score: Score) {
        val userDoc = usersCollection.document(score.uid).get().await()
        val currentUser = userDoc.toObject(User::class.java)
        
        if (currentUser != null) {
            val updates = mutableMapOf<String, Any>()
            if (score.score > currentUser.highScore) updates["highScore"] = score.score
            if (score.topSpeedReached > currentUser.topSpeed) updates["topSpeed"] = score.topSpeedReached
            
            // Simple logic: update total distance
            updates["totalDistance"] = currentUser.totalDistance + score.score
            updates["updatedAt"] = System.currentTimeMillis()

            if (updates.isNotEmpty()) {
                usersCollection.document(score.uid).update(updates).await()
            }
        }
    }

    override suspend fun getTopScores(limit: Int): Result<List<Score>> = runCatching {
        scoresCollection
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(Score::class.java)
    }

    // New helper to get sorted scores by category
    suspend fun getTopScoresByCategory(category: String, limit: Int = 100): List<Score> {
        val field = when(category) {
            "DISTANCE" -> "score"
            "TOP_SPEED" -> "topSpeedReached"
            "AVG_SPEED" -> "avgSpeedDuringRace"
            else -> "score"
        }
        return scoresCollection
            .orderBy(field, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(Score::class.java)
    }

    override suspend fun getUserData(uid: String): Result<User?> = runCatching {
        usersCollection.document(uid).get().await().toObject(User::class.java)
    }

    override suspend fun createOrUpdateUser(user: User): Result<Unit> = runCatching {
        usersCollection.document(user.uid).set(user).await()
    }
}
