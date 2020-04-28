package com.luisromanbcn.gaia.docker

import com.github.dockerjava.api.command.DockerCmdExecFactory
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory
import com.luisromanbcn.gaia.models.ContainerCreationResponse
import com.luisromanbcn.gaia.models.DockerClientInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.io.FileNotFoundException


@Configuration
open class DockerConfiguration {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Bean
    open fun createClientList(): MutableList<DockerClientInfo> = getDockerClientInfoList()

    @Bean
    open fun createContainerList(): MutableList<ContainerCreationResponse> = mutableListOf()

    private fun getDockerClientInfoList(): MutableList<DockerClientInfo> {
        val dockerHostList = System.getProperty("DOCKER_HOST_LIST") ?: System.getProperty("user.dir").replace("%20", " ") + "/dockerhost.txt"
        val lineList = mutableListOf<String>()
        try {
            File(dockerHostList).inputStream().bufferedReader().useLines { lines -> lines.forEach { lineList.add(it)} }
        } catch (e: FileNotFoundException) {
            log.info("DockerHost file is not created, if you want to add hosts when the app starts, please create dockerhost.txt file with desired hosts")
        }
        val clientList: MutableList<DockerClientInfo> = mutableListOf()
        runBlocking {
            lineList.map {
                GlobalScope.async {
                    try {
                        val dockerCmdExecFactory: DockerCmdExecFactory = JerseyDockerCmdExecFactory()
                            .withConnectTimeout(3000)
                        val dockerClient = DockerClientBuilder.getInstance(it)
                            .withDockerCmdExecFactory(dockerCmdExecFactory).build()
                        val info = dockerClient.infoCmd().exec()
                        clientList.add(DockerClientInfo(it, dockerClient, info))
                    } catch (e: Exception) {
                        log.error(e.message)
                    }
                }
            }.awaitAll()
        }
        return clientList
    }

}