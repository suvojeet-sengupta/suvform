package com.suvojeetsengupta.suvform.data.repository

import com.suvojeetsengupta.suvform.data.remote.AdminAddRequest
import com.suvojeetsengupta.suvform.data.remote.AdminAdminsDto
import com.suvojeetsengupta.suvform.data.remote.AdminFormDetailDto
import com.suvojeetsengupta.suvform.data.remote.AdminFormsDto
import com.suvojeetsengupta.suvform.data.remote.AdminStatsDto
import com.suvojeetsengupta.suvform.data.remote.AdminUserDetailDto
import com.suvojeetsengupta.suvform.data.remote.AdminUsersDto
import com.suvojeetsengupta.suvform.data.remote.ResponsesListDto
import com.suvojeetsengupta.suvform.data.remote.SaveFormRequest
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
import com.suvojeetsengupta.suvform.data.remote.UpdateAckDto
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val api: SuvFormApi,
) {
    suspend fun getStats(): Result<AdminStatsDto> = runCatching { api.adminStats() }

    suspend fun listUsers(limit: Int = 50, offset: Int = 0): Result<AdminUsersDto> =
        runCatching { api.adminListUsers(limit, offset) }

    suspend fun listForms(limit: Int = 50, offset: Int = 0): Result<AdminFormsDto> =
        runCatching { api.adminListForms(limit, offset) }

    suspend fun listAdmins(): Result<AdminAdminsDto> = runCatching { api.adminListAdmins() }

    suspend fun addAdminByEmail(email: String): Result<JsonObject> =
        runCatching { api.adminAddAdmin(AdminAddRequest(email = email)) }

    suspend fun addAdmin(uid: String): Result<JsonObject> =
        runCatching { api.adminAddAdmin(AdminAddRequest(uid = uid)) }

    suspend fun removeAdmin(uid: String): Result<JsonObject> =
        runCatching { api.adminRemoveAdmin(uid) }

    suspend fun getUser(uid: String): Result<AdminUserDetailDto> =
        runCatching { api.adminGetUser(uid) }

    suspend fun listUserForms(uid: String, limit: Int = 50, offset: Int = 0): Result<AdminFormsDto> =
        runCatching { api.adminListUserForms(uid, limit, offset) }

    suspend fun getForm(id: String): Result<AdminFormDetailDto> =
        runCatching { api.adminGetForm(id) }

    suspend fun updateForm(id: String, body: SaveFormRequest): Result<UpdateAckDto> =
        runCatching { api.adminUpdateForm(id, body) }

    suspend fun listFormResponses(id: String, limit: Int = 50, offset: Int = 0): Result<ResponsesListDto> =
        runCatching { api.adminListFormResponses(id, limit, offset) }

    suspend fun adminDeleteResponse(formId: String, responseId: String): Result<JsonObject> =
        runCatching { api.adminDeleteResponse(formId, responseId) }

    suspend fun adminDeleteResponses(formId: String, responseIds: List<String>): Result<JsonObject> =
        runCatching { api.adminDeleteResponses(formId, com.suvojeetsengupta.suvform.data.remote.DeleteResponsesRequest(ids = responseIds)) }

    suspend fun adminDeleteAllResponses(formId: String): Result<JsonObject> =
        runCatching { api.adminDeleteResponses(formId, com.suvojeetsengupta.suvform.data.remote.DeleteResponsesRequest(all = true)) }

    suspend fun deleteForm(id: String): Result<JsonObject> =
        runCatching { api.adminDeleteForm(id) }

    suspend fun deleteUser(uid: String): Result<JsonObject> =
        runCatching { api.adminDeleteUser(uid) }

    suspend fun quickAdminCheck(): Result<JsonObject> = runCatching { api.adminCheck() }

    /**
     * Verifies if the current user still has admin access.
     * Returns true if still admin, false if access was revoked.
     */
    suspend fun verifyAdminAccess(): Result<Boolean> = runCatching {
        api.adminCheck()
        true
    }.recoverCatching { throwable ->
        // If we get 403 with admin_revoked, treat as revoked
        val message = throwable.message ?: ""
        if (message.contains("admin_revoked", ignoreCase = true) ||
            message.contains("forbidden", ignoreCase = true)) {
            false
        } else {
            throw throwable
        }
    }
}
