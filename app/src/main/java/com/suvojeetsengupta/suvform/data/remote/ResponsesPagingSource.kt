package com.suvojeetsengupta.suvform.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ResponsesPagingSource @AssistedInject constructor(
    private val api: SuvFormApi,
    @Assisted private val formId: String,
    // Reports total_count from the first page so the screen header doesn't need
    // a separate listResponses(limit=1) call just to show the count.
    @Assisted private val onTotalCount: (Int) -> Unit,
) : PagingSource<Int, ResponseItemDto>() {

    @AssistedFactory
    interface Factory {
        fun create(formId: String, onTotalCount: (Int) -> Unit): ResponsesPagingSource
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ResponseItemDto> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize

            val response = api.listResponses(
                id = formId,
                limit = limit,
                offset = offset
            )

            if (offset == 0) onTotalCount(response.totalCount)

            LoadResult.Page(
                data = response.responses,
                prevKey = if (offset == 0) null else offset - limit,
                nextKey = if (response.hasMore) offset + response.responses.size else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ResponseItemDto>): Int? {
        val pageSize = state.config.pageSize
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(pageSize)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(pageSize)
        }
    }
}
