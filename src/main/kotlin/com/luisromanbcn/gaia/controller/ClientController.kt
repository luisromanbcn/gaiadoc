package com.luisromanbcn.gaia.controller

import com.luisromanbcn.gaia.docker.ClientComponent
import com.luisromanbcn.gaia.models.ClientBasicConfig
import com.luisromanbcn.gaia.models.ClientDeletedResponse
import com.luisromanbcn.gaia.models.ClientStatusInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/client/v1")
class ClientController {

    @Autowired
    private lateinit var clientComponent: ClientComponent

    @PostMapping("/addClientBasic")
    fun addClientBasic(@RequestBody clientBasicConfig: ClientBasicConfig): ClientStatusInfo =
        clientComponent.connectBasicInstance(clientBasicConfig.host)

    @GetMapping("/getClientList")
    fun getClientList(): List<ClientStatusInfo> = clientComponent.getClientList()

    @DeleteMapping("/deleteClient/{hostId}")
    fun deleteClient(@PathVariable("hostId") hostId: String): ClientDeletedResponse =
        clientComponent.removeClient(hostId)

}