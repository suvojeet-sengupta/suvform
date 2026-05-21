package com.suvojeetsengupta.suvform.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import javax.inject.Inject

class ResponsesPagingSource @Inject constructor(
    private val api: SuvFormApi,
    private val formId: String
) : PagingSource<Int, ResponseItemDto>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ResponseItemDto> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize

            val response = api.listResponses(
                id = formId,
                limit = limit,
                offset = offset
            )

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
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(50)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(50)
        }
    }
}
