package com.example.gramasanjeevini

import android.app.Application
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import java.io.File

class GramaSanjeeviniApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // OSMDroid configuration
        // Load default preferences for OSM
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        Configuration.getInstance().load(this, sharedPrefs)
        
        // Set user agent to prevent being banned from OSM servers
        Configuration.getInstance().userAgentValue = packageName
        
        // Configure cache directory
        val osmConfig = Configuration.getInstance()
        val basePath = File(cacheDir, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(basePath, "tiles")
        osmConfig.osmdroidTileCache = tileCache
    }
}
