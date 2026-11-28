package com.pusacoffee.pusatrack.data.api

import com.pusacoffee.pusatrack.data.model.ActivityHistoryItem
import com.pusacoffee.pusatrack.data.model.AppNotification
import com.pusacoffee.pusatrack.data.model.CashBalanceData
import com.pusacoffee.pusatrack.data.model.CashBalanceRequest
import com.pusacoffee.pusatrack.data.model.CreateExpensesRequest
import com.pusacoffee.pusatrack.data.model.CreatePurchaseOrderRequest
import com.pusacoffee.pusatrack.data.model.CreateSaleRequest
import com.pusacoffee.pusatrack.data.model.CreateStockCountRequest
import com.pusacoffee.pusatrack.data.model.CreateUserRequest
import com.pusacoffee.pusatrack.data.model.DashboardSummary
import com.pusacoffee.pusatrack.data.model.DeviceTokenRequest
import com.pusacoffee.pusatrack.data.model.ExpenseBatch
import com.pusacoffee.pusatrack.data.model.ExpenseCategory
import com.pusacoffee.pusatrack.data.model.FinanceSummary
import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.IngredientUpsertRequest
import com.pusacoffee.pusatrack.data.model.LeadTimeStats
import com.pusacoffee.pusatrack.data.model.LoginRequest
import com.pusacoffee.pusatrack.data.model.LoginResponseData
import com.pusacoffee.pusatrack.data.model.Product
import com.pusacoffee.pusatrack.data.model.ProductUpsertRequest
import com.pusacoffee.pusatrack.data.model.PurchaseOrder
import com.pusacoffee.pusatrack.data.model.ReceiveOrderRequest
import com.pusacoffee.pusatrack.data.model.ReportExportRequest
import com.pusacoffee.pusatrack.data.model.ReportExportResponse
import com.pusacoffee.pusatrack.data.model.ResetPasswordRequest
import com.pusacoffee.pusatrack.data.model.Sale
import com.pusacoffee.pusatrack.data.model.StockCountRecord
import com.pusacoffee.pusatrack.data.model.StockInRequest
import com.pusacoffee.pusatrack.data.model.StockMovement
import com.pusacoffee.pusatrack.data.model.UpdateUserRequest
import com.pusacoffee.pusatrack.data.model.UpdateUserStatusRequest
import com.pusacoffee.pusatrack.data.model.User
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // M1 — Auth
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponseData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<LoginResponseData>>

    // M2 — Dashboard Summary
    @GET("dashboard/summary")
    suspend fun getDashboardSummary(
        @Query("date") date: String? = null
    ): Response<ApiResponse<DashboardSummary>>

    // M3 — Sales (Immutable: no PUT/PATCH/DELETE)
    @POST("sales")
    suspend fun createSale(
        @Body request: CreateSaleRequest
    ): Response<ApiResponse<Sale>>

    @GET("sales/{id}")
    suspend fun getSaleDetail(
        @Path("id") id: Int
    ): Response<ApiResponse<Sale>>

    // M4 — Activity History (Read-only: no PUT/PATCH/DELETE)
    @GET("activity-history")
    suspend fun getActivityHistory(
        @Query("user_id") userId: Int? = null,
        @Query("type") type: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<ApiResponse<List<ActivityHistoryItem>>>

    // M6 — Inventory (Ingredients & Stock Movements)
    @GET("ingredients")
    suspend fun getIngredients(
        @Query("status") status: String? = null,
        @Query("q") query: String? = null
    ): Response<ApiResponse<List<Ingredient>>>

    @GET("ingredients/{id}")
    suspend fun getIngredientDetail(
        @Path("id") id: Int
    ): Response<ApiResponse<Ingredient>>

    @POST("ingredients")
    suspend fun createIngredient(
        @Body request: IngredientUpsertRequest
    ): Response<ApiResponse<Ingredient>>

    @PUT("ingredients/{id}")
    suspend fun updateIngredient(
        @Path("id") id: Int,
        @Body request: IngredientUpsertRequest
    ): Response<ApiResponse<Ingredient>>

    @POST("ingredients/{id}/stock-in")
    suspend fun addDirectStock(
        @Path("id") id: Int,
        @Body request: StockInRequest
    ): Response<ApiResponse<Ingredient>>

    @GET("ingredients/{id}/movements")
    suspend fun getIngredientMovements(
        @Path("id") id: Int
    ): Response<ApiResponse<List<StockMovement>>>

    // M11 — Products & Recipes
    @GET("products")
    suspend fun getProducts(): Response<ApiResponse<List<Product>>>

    @GET("products/{id}")
    suspend fun getProductDetail(
        @Path("id") id: Int
    ): Response<ApiResponse<Product>>

    @POST("products")
    suspend fun createProduct(
        @Body request: ProductUpsertRequest
    ): Response<ApiResponse<Product>>

    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Body request: ProductUpsertRequest
    ): Response<ApiResponse<Product>>

    // M7 — Purchase Orders & Lead Time
    @GET("purchase-orders")
    suspend fun getPurchaseOrders(
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<PurchaseOrder>>>

    @GET("purchase-orders/lead-time-stats")
    suspend fun getLeadTimeStats(): Response<ApiResponse<LeadTimeStats>>

    @POST("purchase-orders")
    suspend fun createPurchaseOrder(
        @Body request: CreatePurchaseOrderRequest
    ): Response<ApiResponse<PurchaseOrder>>

    @GET("purchase-orders/{id}")
    suspend fun getPurchaseOrderDetail(
        @Path("id") id: Int
    ): Response<ApiResponse<PurchaseOrder>>

    @POST("purchase-orders/{id}/receive")
    suspend fun receivePurchaseOrder(
        @Path("id") id: Int,
        @Body request: ReceiveOrderRequest
    ): Response<ApiResponse<PurchaseOrder>>

    // M8 — Expenses (Immutable: no PUT/PATCH/DELETE)
    @GET("expense-categories")
    suspend fun getExpenseCategories(): Response<ApiResponse<List<ExpenseCategory>>>

    @POST("expenses")
    suspend fun createExpenses(
        @Body request: CreateExpensesRequest
    ): Response<ApiResponse<ExpenseBatch>>

    // M9 — Finance Summary & Cash Balances
    @GET("finance/summary")
    suspend fun getFinanceSummary(
        @Query("period") period: String,
        @Query("date") date: String? = null
    ): Response<ApiResponse<FinanceSummary>>

    @GET("cash-balances")
    suspend fun getCashBalance(
        @Query("date") date: String? = null
    ): Response<ApiResponse<CashBalanceData>>

    @POST("cash-balances")
    suspend fun saveCashBalance(
        @Body request: CashBalanceRequest
    ): Response<ApiResponse<CashBalanceData>>

    // M6d — Stock Counts (Pemeriksaan Fisik)
    @POST("stock-counts")
    suspend fun submitStockCount(
        @Body request: CreateStockCountRequest
    ): Response<ApiResponse<StockCountRecord>>

    // M6 — Ingredient Photo Upload
    @Multipart
    @POST("ingredients/{id}/image")
    suspend fun uploadIngredientImage(
        @Path("id") id: Int,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<Ingredient>>

    // M10 — Reports & Export (.xlsx)
    @POST("reports/export")
    suspend fun exportReport(
        @Body request: ReportExportRequest
    ): Response<ApiResponse<ReportExportResponse>>

    // M5 — Notifications & FCM Device Token Registration
    @GET("notifications")
    suspend fun getNotifications(): Response<ApiResponse<List<AppNotification>>>

    @POST("notifications/read")
    suspend fun markNotificationsRead(): Response<ApiResponse<Unit>>

    @POST("devices")
    suspend fun registerDeviceToken(
        @Body request: DeviceTokenRequest
    ): Response<ApiResponse<Unit>>

    // M12 — Account Management (L33-L39)
    @GET("users")
    suspend fun getUsers(): Response<ApiResponse<List<User>>>

    @POST("users")
    suspend fun createUser(
        @Body request: CreateUserRequest
    ): Response<ApiResponse<User>>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body request: UpdateUserRequest
    ): Response<ApiResponse<User>>

    @POST("users/{id}/reset-password")
    suspend fun resetUserPassword(
        @Path("id") id: Int,
        @Body request: ResetPasswordRequest
    ): Response<ApiResponse<Unit>>

    @PATCH("users/{id}/status")
    suspend fun updateUserStatus(
        @Path("id") id: Int,
        @Body request: UpdateUserStatusRequest
    ): Response<ApiResponse<User>>
}
