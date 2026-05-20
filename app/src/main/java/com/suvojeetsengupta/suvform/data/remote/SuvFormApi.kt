package com.suvojeetsengupta.suvform.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SuvFormApi {
    @POST("v1/me")
    suspend fun upsertMe(): UserDto

    @GET("v1/forms")
    suspend fun listForms(): FormListDto

    @POST("v1/forms")
    suspend fun createForm(@Body body: CreateFormRequest = CreateFormRequest()): FormSummaryDto
}
