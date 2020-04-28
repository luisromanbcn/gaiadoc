package com.luisromanbcn.gaia.docker

import com.github.dockerjava.api.command.DockerCmdExecFactory
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory
import com.luisromanbcn.gaia.models.ClientDeletedResponse
import com.luisromanbcn.gaia.models.ClientStatusInfo
import com.luisromanbcn.gaia.models.DockerClientInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ClientComponent {

    @Autowired
    private lateinit var clientList: MutableList<DockerClientInfo>

    @Autowired
    private lateinit var containerComponent: ContainerComponent

    fun connectBasicInstance(url: String): ClientStatusInfo {
        val clientStatusInfo: ClientStatusInfo
        val clientFound = clientList.find { it.host == url }
        if (clientFound?.info == null) {
            val dockerCmdExecFactory: DockerCmdExecFactory = JerseyDockerCmdExecFactory()
                .withConnectTimeout(3000)
            val dockerClient = DockerClientInfo(
                url,
                DockerClientBuilder.getInstance(url).withDockerCmdExecFactory(dockerCmdExecFactory).build()
            )
            val info = dockerClient.client.infoCmd().exec()
            dockerClient.info = info
            clientList.add(dockerClient)
            clientStatusInfo = ClientStatusInfo(url, info)
        } else {
            clientStatusInfo = ClientStatusInfo(url, clientFound.info!!)
        }
        return clientStatusInfo
    }

    fun getClientList(): List<ClientStatusInfo> {
        val list = mutableListOf<ClientStatusInfo>()
        runBlocking {
            clientList.map {
                GlobalScope.async {
                    list.add(ClientStatusInfo(it.host, it.client.infoCmd().exec()))
                }
            }.awaitAll()
        }
        return list
    }

    fun removeClient(hostId: String): ClientDeletedResponse {
        val dockerClient = clientList.find { it.info!!.id == hostId }
        var deletedContainers = 0
        if (dockerClient != null) deletedContainers = containerComponent.deleteAllContainersFromHost(dockerClient.info?.id!!)
        return ClientDeletedResponse(clientList.remove(dockerClient), deletedContainers)
    }

}