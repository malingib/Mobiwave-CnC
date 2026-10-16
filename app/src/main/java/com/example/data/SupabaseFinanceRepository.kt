package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class PendingExpenseReview(
    val id: String,
    val merchant: String,
    val amount: Double,
    val category: String,
    val expenseDate: String,
    val invoiceDate: String?,
    val verificationStatus: String,
    val status: String
)

data class FinanceEmployeeOption(val id: String, val employeeNumber: String, val fullName: String)

class SupabaseFinanceRepository {
    private val baseUrl = com.example.BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = com.example.BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun listPendingExpenses(token: String): List<PendingExpenseReview> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/expenses?select=id,merchant_name,gross_amount,category,expense_date,invoice_date,verification_status,status&status=in.(submitted,needs_review)&order=created_at.asc")
            .header("apikey", publishableKey).header("Authorization", "Bearer $token").get().build()
        val array = executeArray(request)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            PendingExpenseReview(
                id = row.getString("id"),
                merchant = row.optString("merchant_name"),
                amount = row.optDouble("gross_amount", 0.0),
                category = row.optString("category"),
                expenseDate = row.optString("expense_date"),
                invoiceDate = row.optString("invoice_date").ifBlank { null },
                verificationStatus = row.optString("verification_status"),
                status = row.optString("status")
            )
        }
    }

    suspend fun listEmployees(token: String): List<FinanceEmployeeOption> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/employees?select=id,employee_number,full_name&status=eq.active&order=full_name.asc")
            .header("apikey", publishableKey).header("Authorization", "Bearer $token").get().build()
        val array = executeArray(request)
        (0 until array.length()).map { i ->
            val row = array.getJSONObject(i)
            FinanceEmployeeOption(row.getString("id"), row.optString("employee_number"), row.optString("full_name"))
        }
    }

    suspend fun reviewExpense(token: String, expenseId: String, decision: String, reason: String): JSONObject =
        withContext(Dispatchers.IO) {
            require(decision in setOf("accept", "reject", "needs_review")) { "Unsupported review decision." }
            require(reason.trim().length >= 5) { "Enter a review reason of at least five characters." }
            rpc(token, "review_expense", JSONObject()
                .put("p_expense_id", expenseId).put("p_decision", decision).put("p_reason", reason.trim()))
        }

    suspend fun recordCashReturn(token: String, employeeId: String, amount: Double, reference: String, reason: String): JSONObject =
        withContext(Dispatchers.IO) {
            require(amount.isFinite() && amount > 0) { "Return amount must be greater than zero." }
            require(reference.trim().length >= 3) { "Enter a return reference of at least three characters." }
            require(reason.trim().length >= 5) { "Enter a reason of at least five characters." }
            rpc(token, "record_cash_return", JSONObject()
                .put("p_employee_id", employeeId).put("p_amount", amount)
                .put("p_reference", reference.trim()).put("p_reason", reason.trim()))
        }

    suspend fun rebuildReconciliation(token: String, employeeId: String, start: String, end: String): JSONObject =
        withContext(Dispatchers.IO) {
            val startDate = try { LocalDate.parse(start) } catch (_: Exception) { throw IOException("Period start must use YYYY-MM-DD.") }
            val endDate = try { LocalDate.parse(end) } catch (_: Exception) { throw IOException("Period end must use YYYY-MM-DD.") }
            require(!endDate.isBefore(startDate)) { "Period end cannot be before period start." }
            rpc(token, "rebuild_reconciliation", JSONObject()
                .put("p_employee_id", employeeId).put("p_period_start", start).put("p_period_end", end))
        }

    private fun rpc(token: String, name: String, payload: JSONObject): JSONObject {
        val request = Request.Builder().url("$baseUrl/rest/v1/rpc/$name")
            .header("apikey", publishableKey).header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMedia)).build()
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val parsed = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
            if (!response.isSuccessful) throw IOException(errorMessage(raw, response.code))
            return parsed
        }
    }

    private fun executeArray(request: Request): JSONArray {
        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException(errorMessage(raw, response.code))
            return JSONArray(raw)
        }
    }

    private fun errorMessage(raw: String, status: Int): String {
        val json = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        return json.optString("message").ifBlank {
            json.optString("details").ifBlank { json.optString("hint").ifBlank { "Request failed ($status). Check your account permissions." } }
        }
    }
}
