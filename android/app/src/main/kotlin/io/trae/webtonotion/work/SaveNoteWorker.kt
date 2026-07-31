package io.trae.webtonotion.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.trae.webtonotion.data.repository.NoteRepository
import java.util.concurrent.TimeUnit

class SaveNoteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(KEY_NOTE_ID, -1)
        if (noteId < 0) return Result.failure()

        val repository = NoteRepository.getInstance(applicationContext)
        val success = repository.syncNote(noteId)

        return if (success) Result.success()
        else if (runAttemptCount < 3) Result.retry()
        else Result.failure()
    }

    companion object {
        const val KEY_NOTE_ID = "note_id"
        private const val WORK_NAME_PREFIX = "save_note_"

        fun enqueue(context: Context, noteId: Long) {
            val request = OneTimeWorkRequestBuilder<SaveNoteWorker>()
                .setInputData(workDataOf(KEY_NOTE_ID to noteId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$WORK_NAME_PREFIX$noteId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
