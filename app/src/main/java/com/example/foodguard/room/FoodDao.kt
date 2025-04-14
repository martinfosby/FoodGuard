package com.example.foodguard.room

import androidx.room.*
import com.example.foodguard.room.Food

@Dao
interface FoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: Food)

    @Query("SELECT * FROM food")
    suspend fun getAllFood(): List<Food>

    @Query("SELECT * FROM food WHERE id = :foodId")
    suspend fun getFood(foodId: Int): Food?

    @Query("DELETE FROM food WHERE id = :foodId")
    suspend fun deleteFood(foodId: Int)
}


