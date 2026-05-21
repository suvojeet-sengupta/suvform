package com.suvojeetsengupta.suvform.data.repository

import com.suvojeetsengupta.suvform.data.remote.FormDetailDto
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches full form details in-memory so multiple screens (Home → Editor,
 * Home → Share, Responses) don't each re-fetch the same form over the network.
 *
 * Detail is small and short-lived; a 60s TTL keeps it fresh while collapsing the
 * burst of repeated getForm() calls that happen while navigating between screens.
 */
@Singleton
class FormRepository @Inject constructor(
    private val api: SuvFormApi,
) {
    private data class Entry(val detail: FormDetailDto, val cachedAt: Long)

    private val cache = ConcurrentHashMap<String, Entry>()
    private val ttlMs = 60_000L

    /** Returns a cached detail if fresh, otherwise fetches and caches it. */
    suspend fun getForm(id: String, forceRefresh: Boolean = false): FormDetailDto {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            cache[id]?.let { if (now - it.cachedAt < ttlMs) return it.detail }
        }
        val detail = api.getForm(id)
        cache[id] = Entry(detail, now)
        return detail
    }

    /** Drop a stale entry after an edit/publish/delete so the next read re-fetches. */
    fun invalidate(id: String) {
        cache.remove(id)
    }
}
