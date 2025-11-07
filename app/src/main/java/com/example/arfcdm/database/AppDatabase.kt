package com.example.arfcdm.database

import android.content.Context
import androidx.room.*

@Entity(tableName = "tools")
data class Tool(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val usage: String,
    val category: String,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface ToolDao {
    @Query("SELECT * FROM tools ORDER BY name ASC")
    suspend fun getAllTools(): List<Tool>

    @Query("SELECT * FROM tools WHERE id = :id")
    suspend fun getToolById(id: Long): Tool?

    @Query("SELECT * FROM tools WHERE name = :name LIMIT 1")
    suspend fun getToolByName(name: String): Tool?

    @Query("SELECT COUNT(*) FROM tools")
    suspend fun getToolCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tool: Tool): Long

    @Update
    suspend fun update(tool: Tool)

    @Delete
    suspend fun delete(tool: Tool)

    @Query("SELECT * FROM tools WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun searchTools(query: String): List<Tool>
}

@Database(entities = [Tool::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun toolDao(): ToolDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tool_recognition_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}