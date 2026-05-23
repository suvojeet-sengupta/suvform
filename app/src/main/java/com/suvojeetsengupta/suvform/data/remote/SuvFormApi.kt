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

    /** Sign out everywhere — invalidates all existing sessions server-side. */
    @POST("v1/me/revoke-sessions")
    suspend fun revokeSessions(): JsonObject

    /** Delete the account and all associated data (forms + responses). */
    @DELETE("v1/me")
    suspend fun deleteAccount(): JsonObject

    /** Download all of the user's data as JSON (GDPR data portability). */
    @retrofit2.http.Streaming
    @GET("v1/me/export")
    suspend fun exportData(): okhttp3.ResponseBody

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

    @POST("v1/forms/{id}/publish")
    suspend fun publishForm(@Path("id") id: String): PublishResponse

    @POST("v1/forms/{id}/unpublish")
    suspend fun unpublishForm(@Path("id") id: String): JsonObject

    @GET("v1/forms/{id}/responses")
    suspend fun listResponses(
        @Path("id") id: String,
        @retrofit2.http.Query("limit") limit: Int = 50,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): ResponsesListDto

    @POST("v1/forms/{id}/insights")
    suspend fun getInsights(@Path("id") id: String): InsightsDto

    // ===================== ADMIN ENDPOINTS =====================

    @GET("v1/admin/me")
    suspend fun adminCheck(): JsonObject

    @GET("v1/admin/stats")
    suspend fun adminStats(): AdminStatsDto

    @GET("v1/admin/users")
    suspend fun adminListUsers(
        @retrofit2.http.Query("limit") limit: Int = 50,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): AdminUsersDto

    @GET("v1/admin/forms")
    suspend fun adminListForms(
        @retrofit2.http.Query("limit") limit: Int = 50,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): AdminFormsDto

    @GET("v1/admin/admins")
    suspend fun adminListAdmins(): AdminAdminsDto

    @POST("v1/admin/admins")
    suspend fun adminAddAdmin(@Body body: AdminAddRequest): JsonObject

    @DELETE("v1/admin/admins/{uid}")
    suspend fun adminRemoveAdmin(@Path("uid") uid: String): JsonObject

    @GET("v1/admin/users/{uid}")
    suspend fun adminGetUser(@Path("uid") uid: String): AdminUserDetailDto

    @GET("v1/admin/users/{uid}/forms")
    suspend fun adminListUserForms(
        @Path("uid") uid: String,
        @retrofit2.http.Query("limit") limit: Int = 50,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): AdminFormsDto

    @GET("v1/admin/forms/{id}")
    suspend fun adminGetForm(@Path("id") id: String): AdminFormDetailDto

    @PUT("v1/admin/forms/{id}")
    suspend fun adminUpdateForm(@Path("id") id: String, @Body body: SaveFormRequest): UpdateAckDto

    @DELETE("v1/admin/forms/{id}")
    suspend fun adminDeleteForm(@Path("id") id: String): JsonObject

    @DELETE("v1/admin/users/{uid}")
    suspend fun adminDeleteUser(@Path("uid") uid: String): JsonObject
}
