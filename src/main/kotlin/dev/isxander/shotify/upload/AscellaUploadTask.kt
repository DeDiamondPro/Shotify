package dev.isxander.shotify.upload

import dev.isxander.shotify.config.ShotifyConfig
import dev.isxander.shotify.util.Screenshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClients
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

object AscellaUploadTask : UploadTask {
    private val json = Json { ignoreUnknownKeys = true }

    override fun upload(screenshot: Screenshot): Screenshot {
        val httpClient = HttpClients.createDefault()
        val uploadFile = HttpPost("https://ascella.wtf/v2/ascella/upload")
        uploadFile.addHeader("x-user-id", ShotifyConfig.ascellaUserId)
        uploadFile.addHeader("x-user-token", ShotifyConfig.ascellaUserToken)
        val builder = MultipartEntityBuilder.create()

        builder.addBinaryBody(
            "image",
            screenshot.image.bytes
        )

        val multipart = builder.build()
        uploadFile.entity = multipart
        val response = httpClient.execute(uploadFile)
        val responseEntity = response.entity

        val textBuilder = StringBuilder()
        BufferedReader(InputStreamReader(responseEntity.content)).use { reader ->
            var c: Int
            while (reader.read().also { c = it } != -1) {
                textBuilder.append(c.toChar())
            }
        }
        val r = textBuilder.toString();
        val json = json.decodeFromString<AscellaResponse>(r)

        if (json.code == 200 && json.success) {
            return Screenshot(screenshot.image, screenshot.file, URL(json.url))
        } else {
            throw RuntimeException("Failed to upload image to Ascella: ${json.code}")
        }
    }

    @Serializable
    data class AscellaResponse(val code: Int, val success: Boolean, val url: String)
}
