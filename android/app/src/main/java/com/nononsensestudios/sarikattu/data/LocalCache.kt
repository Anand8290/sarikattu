package com.nononsensestudios.sarikattu.data

import com.nononsensestudios.sarikattu.domain.GroupDetail
import com.nononsensestudios.sarikattu.domain.GroupSummary
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Last-known server data as JSON files, so groups and balances still show
 * when the phone is offline. Writes always go to the server first.
 */
class LocalCache(private val dir: File) {
    private val json = Json { ignoreUnknownKeys = true }

    fun readGroups(): List<GroupSummary> = read("groups.json") ?: emptyList()
    fun writeGroups(groups: List<GroupSummary>) = write("groups.json", groups)

    fun readGroup(id: String): GroupDetail? = read("group-$id.json")
    fun writeGroup(detail: GroupDetail) = write("group-${detail.group.id}.json", detail)
    fun deleteGroup(id: String) { File(dir, "group-$id.json").delete() }

    fun clear() { dir.listFiles()?.forEach { it.delete() } }

    private inline fun <reified T> read(name: String): T? = runCatching {
        json.decodeFromString<T>(File(dir, name).readText())
    }.getOrNull()

    private inline fun <reified T> write(name: String, value: T) {
        runCatching {
            dir.mkdirs()
            File(dir, name).writeText(json.encodeToString(value))
        }
    }
}
