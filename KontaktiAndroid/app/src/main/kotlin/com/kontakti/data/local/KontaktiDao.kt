package com.kontakti.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KontaktiDao {

    // ── People ────────────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertPeople(people: List<PersonEntity>)

    @Query("SELECT * FROM people ORDER BY full_name ASC")
    fun getPeople(): Flow<List<PersonEntity>>

    @Query("""
        SELECT * FROM people
        WHERE full_name LIKE '%' || :query || '%'
           OR email     LIKE '%' || :query || '%'
           OR company_name LIKE '%' || :query || '%'
        ORDER BY full_name ASC
    """)
    fun searchPeople(query: String): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE email IN (:emails)")
    suspend fun getPeopleByEmail(emails: List<String>): List<PersonEntity>

    // ── Companies ─────────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertCompanies(companies: List<CompanyEntity>)

    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun getCompanies(): Flow<List<CompanyEntity>>

    @Query("""
        SELECT * FROM companies
        WHERE name   LIKE '%' || :query || '%'
           OR domain LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchCompanies(query: String): Flow<List<CompanyEntity>>

    // ── Discussions ───────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertDiscussions(discussions: List<DiscussionEntity>)

    @Query("SELECT * FROM discussions ORDER BY date DESC")
    fun getDiscussions(): Flow<List<DiscussionEntity>>

    // ── Sync queue ────────────────────────────────────────────────────────────

    @Insert
    suspend fun enqueueSyncItem(item: PendingSyncEntity): Long

    @Query("SELECT * FROM pending_sync ORDER BY created_at ASC")
    suspend fun getPendingSyncItems(): List<PendingSyncEntity>

    @Delete
    suspend fun deleteSyncItem(item: PendingSyncEntity)
}
