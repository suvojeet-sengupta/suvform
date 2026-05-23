package com.suvojeetsengupta.suvform.data.repository

import com.suvojeetsengupta.suvform.data.remote.AdminAddRequest
import com.suvojeetsengupta.suvform.data.remote.AdminAdminsDto
import com.suvojeetsengupta.suvform.data.remote.AdminFormsDto
import com.suvojeetsengupta.suvform.data.remote.AdminStatsDto
import com.suvojeetsengupta.suvform.data.remote.AdminUsersDto
import com.suvojeetsengupta.suvform.data.remote.SuvFormApi
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

    suspend fun addAdmin(uid: String): Result<JsonObject> =
        runCatching { api.adminAddAdmin(AdminAddRequest(uid)) }

    suspend fun removeAdmin(uid: String): Result<JsonObject> =
        runCatching { api.adminRemoveAdmin(uid) }

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
