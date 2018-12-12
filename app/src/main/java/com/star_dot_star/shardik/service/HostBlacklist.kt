package com.star_dot_star.shardik.service

import android.content.Context
import com.star_dot_star.shardik.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


/**
 * Read text file in res/raw/ads_blacklist, one line per blacklisted domain
 */
class HostBlacklist(context: Context) {
    private val appContext = context.applicationContext

    private val blacklistedHosts: List<String> by lazy {
        readRawTextFile(R.raw.ads_blacklist)
    }

    fun isHostBlackListed(hostName: String): Boolean {
        return blacklistedHosts.contains(hostName)
    }

    private fun readRawTextFile(resId: Int): List<String> {
        val outList = mutableListOf<String>()
        val inputStream = appContext.resources.openRawResource(resId)

        val inputreader = InputStreamReader(inputStream)
        val buffreader = BufferedReader(inputreader)

        try {
            while (true) {
                val line = buffreader.readLine() ?: break
                outList.add(line)
            }
        } catch (e: IOException) {
            // TODO: error handling
            return emptyList()
        }

        // TODO: adding an arbitrary domain, this is just for testing
        outList.add("www.reddit.com")
        outList.add("reddit.com")

        return outList.toList()
    }
}