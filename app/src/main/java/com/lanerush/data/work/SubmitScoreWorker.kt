package com.lanerush.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.lanerush.data.leaderboard.LeaderboardRepositoryImpl
import com.lanerush.domain.model.Score

/**
 * WorkManager worker that submits a [Score] to Firestore and retries automatically
 * when the device is offline or the request fails transiently.
 *
 * Callers enqueue this via [ScoreSubmitRequest.enqueue]; they never instantiate it directly.
 *
 * Input data keys are defined as constants in [Keys] so both sides stay in sync.
 */
class SubmitScoreWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    object Keys {
        const val UID                 = "uid"
        const val DISPLAY_NAME        = "displayName"
        const val PHOTO_URL           = "photoUrl"
        const val SCORE               = "score"
        const val TOP_SPEED_REACHED   = "topSpeedReached"
        const val AVG_SPEED_RACE      = "avgSpeedDuringRace"
        const val TIMESTAMP           = "timestamp"
    }

    override suspend fun doWork(): Result {
        val score = Score(
            uid                = inputData.getString(Keys.UID)          ?: return Result.failure(),
            displayName        = inputData.getString(Keys.DISPLAY_NAME) ?: "",
            photoUrl           = inputData.getString(Keys.PHOTO_URL)    ?: "",
            score              = inputData.getInt(Keys.SCORE, 0),
            topSpeedReached    = inputData.getDouble(Keys.TOP_SPEED_REACHED, 0.0).toFloat(),
            avgSpeedDuringRace = inputData.getDouble(Keys.AVG_SPEED_RACE,   0.0).toFloat(),
            timestamp          = inputData.getLong(Keys.TIMESTAMP, System.currentTimeMillis())
        )

        val repo   = LeaderboardRepositoryImpl(FirebaseFirestore.getInstance())
        val result = repo.submitScore(score)

        return if (result.isSuccess) {
            Result.success()
        } else {
            // Retry up to WorkManager's default back-off ceiling (~18 000 seconds)
            Result.retry()
        }
    }
}