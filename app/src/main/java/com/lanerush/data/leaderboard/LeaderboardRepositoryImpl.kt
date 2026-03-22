package com.lanerush.data.leaderboard

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lanerush.domain.model.LeaderboardCategory
import com.lanerush.domain.model.Score
import com.lanerush.domain.model.User
import com.lanerush.domain.repository.LeaderboardRepository
import kotlinx.coroutines.tasks.await

class LeaderboardRepositoryImpl(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : LeaderboardRepository {

    private val usersCollection  = firestore.collection("users")
    private val scoresCollection = firestore.collection("scores")

    override suspend fun submitScore(score: Score): Result<Unit> = runCatching {
        val existingDoc   = scoresCollection.document(score.uid).get().await()
        val existingScore = existingDoc.toObject(Score::class.java)

        if (existingScore == null) {
            scoresCollection.document(score.uid).set(score).await()
        } else {
            val updates = mutableMapOf<String, Any>()
            if (score.score              > existingScore.score)              updates["score"]              = score.score
            if (score.topSpeedReached    > existingScore.topSpeedReached)    updates["topSpeedReached"]    = score.topSpeedReached
            if (score.avgSpeedDuringRace > existingScore.avgSpeedDuringRace) updates["avgSpeedDuringRace"] = score.avgSpeedDuringRace

            updates["timestamp"]   = System.currentTimeMillis()
            updates["displayName"] = score.displayName
            updates["photoUrl"]    = score.photoUrl

            if (updates.isNotEmpty()) {
                scoresCollection.document(score.uid).update(updates).await()
            }
        }

        updateUserProfile(score)
    }

    private suspend fun updateUserProfile(score: Score) {
        val userDoc     = usersCollection.document(score.uid).get().await()
        val currentUser = userDoc.toObject(User::class.java) ?: return

        val updates = mutableMapOf<String, Any>()
        if (score.score           > currentUser.highScore) updates["highScore"] = score.score
        if (score.topSpeedReached > currentUser.topSpeed)  updates["topSpeed"]  = score.topSpeedReached

        updates["totalDistance"] = currentUser.totalDistance + score.score
        updates["updatedAt"]     = System.currentTimeMillis()

        if (updates.isNotEmpty()) {
            usersCollection.document(score.uid).update(updates).await()
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

    // FIX: now implements the interface method — LeaderboardViewModel no longer needs to downcast
    override suspend fun getTopScoresByCategory(
        category: LeaderboardCategory,
        limit: Int
    ): Result<List<Score>> = runCatching {
        val field = when (category) {
            LeaderboardCategory.DISTANCE  -> "score"
            LeaderboardCategory.TOP_SPEED -> "topSpeedReached"
            LeaderboardCategory.AVG_SPEED -> "avgSpeedDuringRace"
        }
        scoresCollection
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