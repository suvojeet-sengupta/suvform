package com.suvojeetsengupta.suvform.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import kotlinx.serialization.json.JsonObject

interface SuvFormApi {
    @POST("v1/me")
    suspend fun upsertMe(): UserDto

    @GET("v1/forms")
    suspend fun listForms(): FormListDto

    @POST("v1/forms")
    suspend fun createForm(@Body body: SaveFormRequest): FormDetailDto

    @GET("v1/forms/{id}")
    suspend fun getForm(@Path("id") id: String): FormDetailDto

    @PUT("v1/forms/{id}")
    suspend fun updateForm(@Path("id") id: String, @Body body: SaveFormRequest): UpdateAckDto

    @DELETE("v1/forms/{id}")
    suspend fun deleteForm(@Path("id") id: String): JsonObject

    @POST("v1/ai/generate-form")
    suspend fun generateForm(@Body body: GenerateFormRequest): GeneratedFormDto
}
