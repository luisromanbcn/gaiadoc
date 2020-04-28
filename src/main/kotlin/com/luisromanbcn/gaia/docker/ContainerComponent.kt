package com.luisromanbcn.gaia.docker

import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.InvocationBuilder.AsyncResultCallback
import com.github.dockerjava.core.command.PullImageResultCallback
import com.luisromanbcn.gaia.models.ClientPerformance
import com.luisromanbcn.gaia.models.ContainerCreation
import com.luisromanbcn.gaia.models.ContainerCreationResponse
import com.luisromanbcn.gaia.models.DockerClientInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.IOException
import kotlin.random.Random


@Component
class ContainerComponent {

    @Autowired
    private lateinit var clientList: MutableList<DockerClientInfo>

    @Autowired
    private lateinit var containerList: MutableList<ContainerCreationResponse>

    fun createContainerInDesiredHost(hostId: String, containerCreation: ContainerCreation): ContainerCreationResponse {
        val clientInfo = clientList.find { it.info!!.id == hostId }!!
        val dockerClient = clientInfo.client
        val image: String
        val tag: String
        if (containerCreation.image.contains(":")) {
            val imageTag = containerCreation.image.split(":")
            image = imageTag[0]
            tag = imageTag[1]
        } else {
            image = containerCreation.image
            tag = "latest"
        }
        dockerClient.pullImageCmd(image).withTag(tag).exec(PullImageResultCallback()).awaitSuccess()
        val container = dockerClient.createContainerCmd(containerCreation.image)
        val hostConfig = HostConfig()
        containerCreation.name?.let { container.withName(it) }
        containerCreation.networkMode?.let { hostConfig.withNetworkMode(it) }
        containerCreation.restartAlways.let { if (it) hostConfig.withRestartPolicy(RestartPolicy.alwaysRestart()) }
        containerCreation.volumes?.let {
            val list = mutableListOf<Volume>()
            it.forEach { on -> list.add(Volume(on)) }
            container.withVolumes(list)
        }
        containerCreation.portBindings?.let {
            val list = mutableListOf<PortBinding>()
            it.forEach { on ->
                if (!on.publicRandomRangePort.isNullOrEmpty()) {
                    val randomPorts = on.publicRandomRangePort.split("-")
                    val randomPort = Random.nextInt(randomPorts[0].toInt(), randomPorts[1].toInt())
                    list.add(PortBinding(Ports.Binding.bindPort(randomPort), ExposedPort(on.privatePort)))
                } else {
                    list.add(PortBinding(Ports.Binding.bindPort(on.publicPort!!), ExposedPort(on.privatePort)))
                }
            }
            hostConfig.withPortBindings(list)
        }
        containerCreation.envVariables?.let { container.withEnv(it) }
        hostConfig.withPrivileged(containerCreation.privileged)
        container.withHostConfig(hostConfig)
        val containerExec = container.exec()
        dockerClient.startContainerCmd(containerExec.id).exec()
        val hostChars = clientInfo.host.toCharArray()
        var host = String()
        hostChars.forEachIndexed { i, char ->
            if (char.toString() == "/" && hostChars[i + 1].isLetterOrDigit()) {
                host = clientInfo.host.substring(i + 1).substringBefore(":")
            }
        }
        var deployedContainer: Container? = null
        for (i in 0 until 10) {
            deployedContainer = dockerClient.listContainersCmd().exec().find { it.id == containerExec.id }
            if (deployedContainer != null) break
        }
        containerList.add(ContainerCreationResponse(clientInfo.info?.id!!, deployedContainer!!))
        return ContainerCreationResponse(host, deployedContainer)
    }

    fun createContainer(containerCreation: ContainerCreation): ContainerCreationResponse {
        val clientPerformance: MutableList<ClientPerformance> = mutableListOf()
        runBlocking {
            clientList.map {
                GlobalScope.async {
                    var usage: Double = 0.toDouble()
                    try {
                        val containers = it.client.listContainersCmd().exec()
                        val infoHost = it.client.infoCmd().exec()
                        containers.map { on ->
                            GlobalScope.async {
                                val callback = AsyncResultCallback<Statistics>()
                                it.client.statsCmd(on.id).exec(callback)
                                var stats: Statistics? = null
                                try {
                                    stats = callback.awaitResult()
                                    callback.close()
                                } catch (e: RuntimeException) {

                                } catch (e: IOException) {

                                }
                                usage += stats!!.memoryStats.usage!!
                            }
                        }.awaitAll()
                        usage = if (containers.size > 0) ((infoHost.memTotal!!.toDouble() - usage) / infoHost.memTotal!!.toDouble()) * 100.toDouble() else 100.toDouble()
                        val cpuUsage = if (infoHost.containersRunning!! >= infoHost.ncpu!!) {
                            0.toDouble()
                        } else {
                            (infoHost.ncpu!!.toDouble() - infoHost.containersRunning!!.toDouble()) / infoHost.ncpu!!.toDouble() * 100.toDouble()
                        }
                        usage += cpuUsage
                    } catch (e: Exception) {

                    }
                    clientPerformance.add(ClientPerformance(it.host, usage))
                }
            }.awaitAll()
        }
        var listPosition = 0
        clientPerformance.forEachIndexed { index, it ->
            if (it.points >= clientPerformance[listPosition].points) listPosition = index
        }
        val dockerClientInfo = clientList.find { it.host == clientPerformance[listPosition].host }!!.info
        return createContainerInDesiredHost(dockerClientInfo?.id!!, containerCreation)
    }

    fun deleteContainer(containerId: String, deleteList: Boolean = true): Boolean {
        val container = containerList.find { it.containerInfo.id == containerId }
        val dockerClient = clientList.find { it.info!!.id == container!!.host }!!.client
        try {
            dockerClient.stopContainerCmd(containerId).exec()
        } catch (e: Exception) {

        }
        var deleted = false
        try {
            dockerClient.removeContainerCmd(containerId).exec()
            deleted = true
        } catch (e: Exception) {

        } finally {
            if (deleteList) containerList.remove(container)
        }
        return deleted
    }

    fun deleteAllContainersFromHost(hostId: String): Int {
        var deletedContainer = 0
        val it: MutableIterator<ContainerCreationResponse> = containerList.iterator()
        while (it.hasNext()) {
            val container = it.next()
            if (container.host == hostId) {
                deleteContainer(container.containerInfo.id, false)
                it.remove()
                deletedContainer++
            }
        }
        return deletedContainer
    }

}