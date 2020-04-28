package com.luisromanbcn.gaia.models

import com.github.dockerjava.api.model.Info

data class ClientStatusInfo(
    val host: String,
    val info: Info
)