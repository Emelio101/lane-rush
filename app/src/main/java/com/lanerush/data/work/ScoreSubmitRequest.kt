package com.lanerush.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lanerush.domain.model.Score
import java.util.concurrent.TimeUnit

/**
 * Enqueues a [SubmitScoreWorker] request with:
 * - CONNECTED network constraint  — work only runs when online
 * - EXPONENTIAL back-off starting at 15 s — survives temporary outages
 * - Unique work tag per UID so duplicate race results don't stack up
 */
object ScoreSubmitRequest {

    fun enqueue(context: Context, score: Score) {
        val data = workDataOf(
            SubmitScoreWorker.Keys.UID               to score.uid,
            SubmitScoreWorker.Keys.DISPLAY_NAME      to score.displayName,
            SubmitScoreWorker.Keys.PHOTO_URL         to score.photoUrl,
            SubmitScoreWorker.Keys.SCORE             to score.score,
            SubmitScoreWorker.Keys.TOP_SPEED_REACHED to score.topSpeedReached.toDouble(),
            SubmitScoreWorker.Keys.AVG_SPEED_RACE    to score.avgSpeedDuringRace.toDouble(),
            SubmitScoreWorker.Keys.TIMESTAMP         to score.timestamp
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SubmitScoreWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag("score_submit_${score.uid}")
            .build()

        // KEEP: if a pending job for this user already exists (e.g. they crashed twice
        // without connectivity), keep the existing work so we don't lose the earlier score.
        WorkManager.getInstance(context).enqueue(request)
    }
}