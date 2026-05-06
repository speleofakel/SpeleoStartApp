package com.speleo.start.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.speleo.start.data.local.entity.AppSettingsEntity

@Dao
interface AppSettingsDao {

    /**
     * Получить значение настройки по ключу
     * @param key Ключ настройки
     * @return Значение или null, если не найдено
     */
    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun get(key: String): String?

    /**
     * Сохранить или обновить настройку
     * @param entity Сущность с ключом и значением
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: AppSettingsEntity)

    /**
     * Удалить настройку по ключу
     * @param key Ключ настройки для удаления
     */
    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun delete(key: String)  // ✅ МЕТОД ЕСТЬ

    /**
     * Получить все настройки (для отладки)
     * @return Список всех настроек
     */
    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettingsEntity>

    /**
     * Очистить все настройки (для сброса приложения)
     */
    @Query("DELETE FROM app_settings")
    suspend fun clearAll()
}