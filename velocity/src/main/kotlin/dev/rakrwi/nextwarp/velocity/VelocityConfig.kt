package dev.rakrwi.nextwarp.velocity

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader

data class VelocityRedisConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String = ""
)

data class VelocityConfig(
    val redis: VelocityRedisConfig = VelocityRedisConfig()
)

object VelocityConfigLoader {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun loadConfig(dataFolder: File): VelocityConfig {
        val configFile = File(dataFolder, "config.json")

        if (!configFile.exists()) {
            dataFolder.mkdirs()
            // 기본 설정 복사
            val defaultConfig = VelocityConfig()
            FileWriter(configFile).use { writer ->
                gson.toJson(defaultConfig, writer)
            }
            return defaultConfig
        }

        return FileReader(configFile).use { reader ->
            gson.fromJson(reader, VelocityConfig::class.java)
        }
    }
}
