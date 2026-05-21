package com.suvojeetsengupta.suvform.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.suvojeetsengupta.suvform.data.remote.InsightsDto
import com.suvojeetsengupta.suvform.data.remote.ResponseItemDto
import com.suvojeetsengupta.suvform.data.remote.ResponsesPagingSource
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for form responses: the paged list, the AI insights
 * call, and the "fetch everything" walk used by CSV/PDF export.
 */
@Singleton
class ResponseRepository @Inject constructor(
    private val api: SuvFormApi,
    private val pagingSourceFactory: ResponsesPagingSource.Factory,
) {
    /** A cached paging flow of responses for [formId]; reports total via [onTotalCount]. */
    fun pagingFlow(
        formId: String,
        scope: CoroutineScope,
        onTotalCount: (Int) -> Unit,
    ): Flow<PagingData<ResponseItemDto>> =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE,
            ),
            pagingSourceFactory = { pagingSourceFactory.create(formId, onTotalCount) },
        ).flow.cachedIn(scope)

    suspend fun getInsights(formId: String): InsightsDto = api.getInsights(formId)

    /**
     * Fetches every response by walking the paginated endpoint until the server
     * reports no more pages, so exports are never silently truncated.
     */
    suspend fun fetchAllResponses(formId: String): List<ResponseItemDto> {
        val all = mutableListOf<ResponseItemDto>()
        var offset = 0
        while (true) {
            val page = api.listResponses(formId, limit = EXPORT_PAGE_SIZE, offset = offset)
            all += page.responses
            if (!page.hasMore || page.responses.isEmpty()) break
            offset += page.responses.size
        }
        return all
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 20
        const val EXPORT_PAGE_SIZE = 200
    }
}
