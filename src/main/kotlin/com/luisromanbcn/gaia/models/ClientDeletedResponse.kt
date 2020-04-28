package com.luisromanbcn.gaia.models

data class ClientDeletedResponse(
    val deleted: Boolean,
    val numberContainersDeleted: Int
)