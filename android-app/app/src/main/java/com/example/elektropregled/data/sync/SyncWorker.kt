package com.example.elektropregled.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.elektropregled.ElektropregledApplication
import com.example.elektropregled.data.repository.PregledRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val app = applicationContext as? ElektropregledApplication ?: return Result.failure()
        val repository = PregledRepository(app.database, app.tokenStorage)
        
        return try {
            val result = repository.syncPregledi()
            when (result) {
                is com.example.elektropregled.data.repository.SyncResult.Success -> {
                    if (result.synced > 0) {
                        Result.success()
                    } else {
                        Result.retry()
                    }
                }
                is com.example.elektropregled.data.repository.SyncResult.Error -> {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
