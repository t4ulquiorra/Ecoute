package com.ecoute.music.utils

import android.util.Log
import com.ecoute.music.Dependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Decodes YouTube's n-parameter throttling in stream URLs.
 *
 * YouTube adds an "n" query parameter to stream URLs that causes throttling
 * when accessed directly. The actual value must be transformed by running
 * a JS function extracted from YouTube's player script.
 *
 * We use the bundled qjs (QuickJS) binary to run this JS function natively.
 */
object NParamDecoder {

    private const val TAG = "NParamDecoder"

    // Cache the extracted JS function across songs — only fetch player JS once per session
    @Volatile private var cachedNFunction: String? = null

    // Regex to find the player JS URL in YouTube's main page
    private val playerJsRegex = Regex("""/s/player/[a-f0-9]+/player_ias[^"']+base\.js""")

    // Regex to extract the n-param function name from player JS
    // Matches patterns like: .get("n"))&&(b=<funcName>(b)
    private val nFuncNameRegex = Regex("""\.get\("n"\)\)&&\(b=([a-zA-Z0-9${'$'}]+)(?:\[(\d+)\])?\([a-zA-Z0-9]\)""")

    /**
     * Given a YouTube stream URL, returns a version with the n-param decoded
     * so it plays without throttling. Returns the original URL on any failure.
     */
    suspend fun decode(url: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val nParam = extractNParam(url) ?: return@withContext url
            val nFunction = getNFunction() ?: return@withContext url
            val decoded = runNFunction(nFunction, nParam) ?: return@withContext url
            url.replace("&n=$nParam", "&n=$decoded")
                .replace("?n=$nParam&", "?n=$decoded&")
        }.onFailure {
            Log.e(TAG, "n-param decode failed", it)
        }.getOrDefault(url)
    }

    private fun extractNParam(url: String): String? {
        val uri = android.net.Uri.parse(url)
        return uri.getQueryParameter("n")?.takeIf { it.isNotBlank() }
    }

    private fun getNFunction(): String? {
        cachedNFunction?.let { return it }

        return runCatching {
            // Step 1: get YouTube homepage to find player JS URL
            Log.d(TAG, "Fetching YouTube homepage to find player JS...")
            val homePage = URL("https://www.youtube.com/music").readText()
            val playerJsPath = playerJsRegex.find(homePage)?.value
                ?: run {
                    Log.e(TAG, "Could not find player JS URL in homepage")
                    return@runCatching null
                }

            val playerJsUrl = "https://www.youtube.com$playerJsPath"
            Log.d(TAG, "Found player JS: $playerJsUrl")

            // Step 2: fetch the player JS
            val playerJs = URL(playerJsUrl).readText()

            // Step 3: find the n-param function name
            val matchResult = nFuncNameRegex.find(playerJs)
                ?: run {
                    Log.e(TAG, "Could not find n-param function name in player JS")
                    return@runCatching null
                }

            val funcName = matchResult.groupValues[1]
            val arrayIndex = matchResult.groupValues[2].toIntOrNull()
            Log.d(TAG, "Found n-param function name: $funcName (array index: $arrayIndex)")

            // Step 4: extract the actual function body
            // Handle both direct function and array-of-functions pattern
            val actualFuncName = if (arrayIndex != null) {
                // Pattern: var funcName=[function(a){...}]; b=funcName[0](b)
                val arrayRegex = Regex("""var ${Regex.escape(funcName)}\s*=\s*\[(.+?)\];""", RegexOption.DOT_MATCHES_ALL)
                val arrayMatch = arrayRegex.find(playerJs)
                if (arrayMatch != null) {
                    // Wrap array content as a named function for qjs
                    val extracted = "var _nfunc=${arrayMatch.groupValues[1]};_nfunc"
                    cachedNFunction = extracted
                    return@runCatching extracted
                }
                funcName
            } else {
                funcName
            }

            // Extract function by finding its definition
            val funcRegex = Regex(
                """(?:function\s+${Regex.escape(actualFuncName)}|var\s+${Regex.escape(actualFuncName)}\s*=\s*function)\s*\([^)]*\)\s*\{""",
                RegexOption.DOT_MATCHES_ALL
            )
            val funcStart = funcRegex.find(playerJs)
                ?: run {
                    Log.e(TAG, "Could not find function body for: $actualFuncName")
                    return@runCatching null
                }

            // Find matching closing brace
            var depth = 0
            var i = funcStart.range.last
            while (i < playerJs.length) {
                when (playerJs[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) break
                    }
                }
                i++
            }

            val funcBody = playerJs.substring(funcStart.range.first, i + 1)
            val jsSnippet = "$funcBody\n$actualFuncName"
            Log.d(TAG, "Extracted n-param function (${funcBody.length} chars)")

            cachedNFunction = jsSnippet
            jsSnippet
        }.onFailure {
            Log.e(TAG, "Failed to extract n-param function", it)
        }.getOrNull()
    }

    private fun runNFunction(jsFunction: String, nParam: String): String? {
        return runCatching {
            val qjsFile = Dependencies.quickjsPath
            if (!qjsFile.exists() || !qjsFile.canExecute()) {
                Log.e(TAG, "qjs binary not found or not executable: ${qjsFile.absolutePath}")
                return@runCatching null
            }

            // Write JS to a temp file
            val tmpJs = File.createTempFile("nparam", ".js").apply {
                deleteOnExit()
                writeText("print(($jsFunction)('$nParam'));")
            }

            val process = ProcessBuilder(qjsFile.absolutePath, tmpJs.absolutePath)
                .redirectErrorStream(true)
                .start()

            val result = process.inputStream.bufferedReader().readText().trim()
            val exited = process.waitFor(5, TimeUnit.SECONDS)
            tmpJs.delete()

            if (!exited || process.exitValue() != 0) {
                Log.e(TAG, "qjs process failed: $result")
                return@runCatching null
            }

            Log.d(TAG, "n-param decoded: $nParam -> $result")
            result.takeIf { it.isNotBlank() && it != nParam }
        }.onFailure {
            Log.e(TAG, "qjs execution failed", it)
        }.getOrNull()
    }

    /** Call this when starting a new session to force re-fetch of player JS */
    fun clearCache() {
        cachedNFunction = null
    }
}
