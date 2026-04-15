package com.trackhub.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/salt")
    suspend fun salt(@Body body: SaltRequest): SaltResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth/me")
    suspend fun me(): MeResponse

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<Unit>

    @POST("auth/recover-start")
    suspend fun recoverStart(@Body body: RecoverStartRequest): RecoverStartResponse

    @POST("auth/recover-complete")
    suspend fun recoverComplete(@Body body: RecoverCompleteRequest): LoginResponse

    @DELETE("auth/account")
    suspend fun deleteAccount(): Response<Unit>

    @GET("records")
    suspend fun listRecords(
        @Query("type") type: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): List<EncryptedRecord>

    @GET("records/{id}")
    suspend fun getRecord(@Path("id") id: String): EncryptedRecord

    @POST("records")
    suspend fun createRecord(@Body body: RecordCreateRequest): EncryptedRecord

    @PUT("records/{id}")
    suspend fun updateRecord(@Path("id") id: String, @Body body: RecordUpdateRequest): EncryptedRecord

    @DELETE("records/{id}")
    suspend fun deleteRecord(@Path("id") id: String): Response<Unit>

    @GET("exercises")
    suspend fun exercises(): List<Exercise>

    @GET("food-search")
    suspend fun foodSearch(@Query("q") query: String): List<FoodSearchItem>
}
