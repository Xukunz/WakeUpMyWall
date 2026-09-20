package com.xukunz.wakeupmywall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.xukunz.wakeupmywall.app.App
import com.xukunz.wakeupmywall.core.storage.JsonSettingsStorage
import com.xukunz.wakeupmywall.data.settings.DataStoreKeyValueStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 平台能力在入口注入：commonMain 只认 SettingsStorage 接口，DataStore 不进 commonMain。
        val storage = JsonSettingsStorage(DataStoreKeyValueStore(applicationContext))
        setContent { App(storage = storage) }
    }
}
