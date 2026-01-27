package com.example.elektropregled.data.api.dto

data class SyncResponse(
    val success: Boolean,
    val message: String,
    val serverPregledId: Long,
    val idMappings: IdMappings,
    val timestamp: String
)

data class IdMappings(
    val pregled: ServerIdMapping,
    val stavke: List<ServerIdMapping>
)

data class ServerIdMapping(
    val lokalniId: String,
    val serverId: Long
)
