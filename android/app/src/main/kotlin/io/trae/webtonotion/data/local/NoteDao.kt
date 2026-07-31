package io.trae.webtonotion.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE status IN ('draft', 'pending', 'failed') ORDER BY createdAt ASC")
    suspend fun getPendingNotes(): List<NoteEntity>

    @Query("UPDATE notes SET status = :status, error = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET notionPageId = :notionPageId, status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNotionPageId(id: Long, notionPageId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET status = :status, notionPageId = :notionPageId, title = :title, content = :content, error = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncResult(id: Long, status: String, notionPageId: String?, title: String, content: String, error: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM notes WHERE status = :status")
    suspend fun countByStatus(status: String): Int
}
