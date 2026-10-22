package com.example.sapp.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UpdateLogicTest {

    private val sample = """
        {
          "versionName": "1.0.0",
          "versionCode": 12,
          "tag": "v1.0.0",
          "apk": "sapp-1.0.0.apk",
          "sha256": "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
          "publishedAt": "2026-10-22T00:00:00Z"
        }
    """.trimIndent()

    @Test
    fun parsesLatestJson() {
        val release = GitHubReleasesClient.parseReleaseMetadata(sample)

        assertNotNull(release)
        assertEquals("1.0.0", release?.versionName)
        assertEquals(12, release?.versionCode ?: -1)
        assertEquals("v1.0.0", release?.tag)
        assertEquals("sapp-1.0.0.apk", release?.apk)
        assertEquals(64, release?.sha256?.length ?: 0)
    }

    @Test
    fun comparesVersionCodes() {
        assertTrue(UpdateVersion.isNewer(12, 11))
        assertFalse(UpdateVersion.isNewer(11, 11))
        assertFalse(UpdateVersion.isNewer(10, 11))
        assertFalse(UpdateVersion.isNewer(null, 11))
    }

    @Test
    fun buildsGitHubAssetUrls() {
        val client = GitHubReleasesClient()

        assertEquals(
            "https://github.com/fuuyuki/sapp-android/releases/latest/download/latest.json",
            client.latestJsonUrl()
        )
        assertEquals(
            "https://github.com/fuuyuki/sapp-android/releases/latest/download/sapp-1.0.0.apk",
            client.apkUrl("sapp-1.0.0.apk")
        )
    }

    @Test
    fun rejectsUnsafeAssetNames() {
        assertTrue(GitHubReleasesClient.isSafeAssetName("sapp-1.0.0.apk"))
        assertFalse(GitHubReleasesClient.isSafeAssetName("../../evil.apk"))
        assertFalse(GitHubReleasesClient.isSafeAssetName("sapp 1.0.0.apk"))
        assertFalse(GitHubReleasesClient.isSafeAssetName(null))
    }

    @Test
    fun computesSha256() {
        val file = File.createTempFile("sapp-sha", ".txt")
        try {
            file.writeText("hello")
            assertEquals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                Sha256.of(file)
            )
        } finally {
            file.delete()
        }
    }
}
