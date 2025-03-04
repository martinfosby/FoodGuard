import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.foodguard.Food
import java.util.Date

@Database(entities = [Food::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "foodguard.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
