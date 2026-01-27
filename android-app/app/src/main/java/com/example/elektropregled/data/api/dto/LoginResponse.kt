package com.example.elektropregled.data.api.dto

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val username: String
)
