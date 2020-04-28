package com.luisromanbcn.gaia.controller

import com.luisromanbcn.gaia.docker.ContainerComponent
import com.luisromanbcn.gaia.models.ContainerCreation
import com.luisromanbcn.gaia.models.ContainerCreationResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/container/v1")
class ContainerController {

    @Autowired
    private lateinit var containerComponent: ContainerComponent

    @Autowired
    private lateinit var containerList: List<ContainerCreationResponse>

    @PostMapping("/createContainer/{hostId}")
    fun createContainerToSpecificHost(@PathVariable("hostId") hostId: String,
                                      @RequestBody containerCreation: ContainerCreation): ContainerCreationResponse =
        containerComponent.createContainerInDesiredHost(hostId, containerCreation)

    @PostMapping("/createContainer")
    fun createContainer(@RequestBody containerCreation: ContainerCreation): ContainerCreationResponse =
        containerComponent.createContainer(containerCreation)

    @DeleteMapping("/deleteContainer/{containerId}")
    fun deleteContainer(@PathVariable("containerId") containerId: String): Boolean =
        containerComponent.deleteContainer(containerId)

    @GetMapping("/containerList")
    fun getContainerList(): List<ContainerCreationResponse> = containerList

}