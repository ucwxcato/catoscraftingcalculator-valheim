package com.cato.resourcecalc.updates

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private const val RELEASES_API = "https://api.github.com/repos/ucwxcato/craftingcalculator-valheim/releases"

@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val name: String? = null,
    val html_url: String,
    val body: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
private data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val content_type: String? = null,
    val size: Long = 0,
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val contentType: String?,
    val size: Long,
)

data class UpdateInfo(
    val version: AppVersion,
    val title: String,
    val releaseUrl: String,
    val notes: String,
    val assets: List<ReleaseAsset>,
    val prerelease: Boolean,
)

data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: List<String>,
) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch).let { if (it != 0) return it }
        if (prerelease.isEmpty() && other.prerelease.isNotEmpty()) return 1
        if (prerelease.isNotEmpty() && other.prerelease.isEmpty()) return -1
        for (index in 0 until minOf(prerelease.size, other.prerelease.size)) {
            val left = prerelease[index]
            val right = other.prerelease[index]
            val comparison = if (left.toIntOrNull() != null && right.toIntOrNull() != null) {
                left.toInt().compareTo(right.toInt())
            } else if (left.toIntOrNull() != null) {
                -1
            } else if (right.toIntOrNull() != null) {
                1
            } else {
                left.compareTo(right)
            }
            if (comparison != 0) return comparison
        }
        return prerelease.size.compareTo(other.prerelease.size)
    }

    override fun toString(): String = buildString {
        append("$major.$minor.$patch")
        if (prerelease.isNotEmpty()) append('-').append(prerelease.joinToString("."))
    }

    companion object {
        private val pattern = Regex("v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?")

        fun parse(raw: String): AppVersion? {
            val match = pattern.matchEntire(raw.trim()) ?: return null
            return AppVersion(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                prerelease = match.groupValues[4].takeIf { it.isNotEmpty() }?.split('.') ?: emptyList(),
            )
        }
    }
}

object ReleaseChecker {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build()

    fun check(currentVersion: AppVersion): UpdateInfo? {
        val request = HttpRequest.newBuilder(URI(RELEASES_API))
            .timeout(Duration.ofSeconds(6))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "CatosResourceCalc/$currentVersion")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) throw IllegalStateException("GitHub Releases returned HTTP ${response.statusCode()}")
        val releases = json.decodeFromString<List<GitHubRelease>>(response.body())
            .asSequence()
            .filterNot { it.draft }
            .mapNotNull { release -> AppVersion.parse(release.tag_name)?.let { it to release } }
            .filter { (version, _) -> version > currentVersion }
            .maxByOrNull { (version, _) -> version }
            ?: return null
        val (version, release) = releases
        return UpdateInfo(
            version = version,
            title = release.name?.takeIf { it.isNotBlank() } ?: release.tag_name,
            releaseUrl = release.html_url,
            notes = release.body.orEmpty(),
            assets = release.assets.map { ReleaseAsset(it.name, it.browser_download_url, it.content_type, it.size) },
            prerelease = release.prerelease,
        )
    }
}
