package com.luisromanbcn.gaia.models

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Info

data class DockerClientInfo(
    val host: String,
    val client: DockerClient,
    var info: Info? = null
)