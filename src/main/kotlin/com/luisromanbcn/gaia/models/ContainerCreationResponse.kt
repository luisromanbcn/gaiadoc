package com.luisromanbcn.gaia.models

import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.model.Container

data class ContainerCreationResponse(
    val host: String,
    val containerInfo: Container
)