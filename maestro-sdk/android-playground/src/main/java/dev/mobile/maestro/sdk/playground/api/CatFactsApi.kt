package dev.mobile.maestro.sdk.playground.api

import dev.mobile.maestro.sdk.playground.api.model.CatBreed
import retrofit2.http.GET

interface CatFactsApi {

    @GET("breeds")
    fun getBreeds(): List<CatBreed>

}