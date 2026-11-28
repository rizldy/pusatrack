package com.pusacoffee.pusatrack.data.api

import com.pusacoffee.pusatrack.data.model.Ingredient
import com.pusacoffee.pusatrack.data.model.IngredientUpsertRequest
import com.pusacoffee.pusatrack.data.model.LoginRequest
import com.pusacoffee.pusatrack.data.model.LoginResponseData
import com.pusacoffee.pusatrack.data.model.Product
import com.pusacoffee.pusatrack.data.model.ProductUpsertRequest
import com.pusacoffee.pusatrack.data.model.StockInRequest
import com.pusacoffee.pusatrack.data.model.StockMovement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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
}
