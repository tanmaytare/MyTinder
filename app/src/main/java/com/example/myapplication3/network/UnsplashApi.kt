package com.example.myapplication3.network

import com.example.myapplication3.model.UnsplashPhoto
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface UnsplashApi {
    @Headers("Authorization: Client-ID Nq9DvBgPO2ooKnedJv0PsmR9lEu01OPQDu5bBmRUjxY") // Replace YOUR_ACCESS_KEY with your Unsplash API access key
    @GET("photos/random")
    suspend fun getRandomPhotos(
        @Query("count") count: Int = 10,
        @Query("query") query: String = "people" // Search for images of people
    ): List<UnsplashPhoto>
}
