package com.suvojeetsengupta.suvform.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.suvojeetsengupta.suvform.data.local.FormDao
import com.suvojeetsengupta.suvform.data.local.FormSummaryEntity
import com.suvojeetsengupta.suvform.data.remote.FormDetailDto
import com.suvojeetsengupta.suvform.data.remote.PublishResponse
import com.suvojeetsengupta.suvform.data.remote.SaveFormRequest
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import com.suvojeetsengupta.suvform.data.remote.UserStatsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for forms. Reads come from Room (offline-first); the
 * network is used to sync the list and fetch/mutate individual forms. Full form
 * details are cached in-memory (60s) so multiple screens (Home → Editor → Share
 * → Responses) reuse the same fetch instead of each hitting the network.
 */
@Singleton
class FormRepository @Inject constructor(
    private val api: SuvFormApi,
    private val formDao: FormDao,
    private val auth: FirebaseAuth,
) {
    private data class Entry(val detail: FormDetailDto, val cachedAt: Long)

    private val detailCache = ConcurrentHashMap<String, Entry>()
    private val detailTtlMs = 60_000L

    // Throttle list syncs so re-entering screens doesn't spam listForms().
    private val syncThrottleMs = 5 * 60_000L
    @Volatile private var lastSyncAt = 0L

    @Volatile private var cachedStats: UserStatsDto? = null

    // ---- List (Room-backed) ----

    /** Observe the current user's cached form summaries. Emits offline too. */
    fun observeForms(): Flow<List<FormSummaryEntity>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return formDao.observeForOwner(uid)
    }

    /**
     * Sync the form list and stats from the server into Room. Throttled to [syncThrottleMs]
     * unless [force] is true. Returns true if a network call actually ran.
     * Throws on network failure (caller decides how to surface it).
     */
    suspend fun syncDashboard(force: Boolean): UserStatsDto? {
        val uid = auth.currentUser?.uid ?: return cachedStats
        val now = System.currentTimeMillis()
        if (!force && now - lastSyncAt < syncThrottleMs && cachedStats != null) return cachedStats

        val resp = api.getUserDashboard()
        formDao.replaceForOwner(uid, resp.forms.map { FormSummaryEntity.fromDto(uid, it) })
        cachedStats = resp.stats
        lastSyncAt = now
        return resp.stats
    }

    suspend fun syncForms(force: Boolean): Boolean {
        syncDashboard(force)
        return true
    }

    fun getCachedStats(): UserStatsDto? = cachedStats

    // ---- Detail (in-memory cache) ----

    /** Returns a cached detail if fresh, otherwise fetches and caches it. */
    suspend fun getForm(id: String, forceRefresh: Boolean = false): FormDetailDto {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            detailCache[id]?.let { if (now - it.cachedAt < detailTtlMs) return it.detail }
        }
        val detail = api.getForm(id)
        detailCache[id] = Entry(detail, now)
        return detail
    }

    // ---- Mutations (keep Room + cache consistent) ----

    suspend fun createForm(req: SaveFormRequest): FormDetailDto = api.createForm(req)

    suspend fun updateForm(id: String, req: SaveFormRequest) {
        api.updateForm(id, req)
        invalidate(id)
    }

    /** Optimistically removes from cache, then deletes server-side. */
    suspend fun deleteForm(id: String) {
        formDao.deleteById(id)
        invalidate(id)
        api.deleteForm(id)
    }

    suspend fun publish(id: String): PublishResponse {
        val r = api.publishForm(id)
        invalidate(id)
        formDao.updateShareUrl(id, r.url)
        return r
    }

    suspend fun unpublish(id: String) {
        api.unpublishForm(id)
        invalidate(id)
    }

    suspend fun cacheShareUrl(id: String, url: String) = formDao.updateShareUrl(id, url)

    /** Drop a stale detail entry so the next read re-fetches. */
    fun invalidate(id: String) {
        detailCache.remove(id)
    }
}
