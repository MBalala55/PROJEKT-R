package com.example.elektropregled.data.api.dto

import com.google.gson.annotations.SerializedName

data class SyncResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("server_pregled_id")
    val serverPregledId: Long,
    @SerializedName("id_mappings")
    val idMappings: IdMappings?,
    val timestamp: String
)

data class IdMappings(
    val pregled: ServerIdMapping?,
    val stavke: List<ServerIdMapping>?
)

data class ServerIdMapping(
    @SerializedName("lokalni_id")
    val lokalniId: String,
    @SerializedName("server_id")
    val serverId: Long
)
