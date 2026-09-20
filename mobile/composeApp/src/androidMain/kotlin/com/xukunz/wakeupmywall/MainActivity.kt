package com.xukunz.wakeupmywall

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.xukunz.wakeupmywall.app.App
import com.xukunz.wakeupmywall.core.agent.KeystoreAgentTokenStore
import com.xukunz.wakeupmywall.core.connectivity.AndroidTcpProbe
import com.xukunz.wakeupmywall.core.storage.JsonSettingsStorage
import com.xukunz.wakeupmywall.core.wol.UdpWakeOnLanSender
import com.xukunz.wakeupmywall.data.settings.DataStoreKeyValueStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 平台能力在入口注入：commonMain 只认 SettingsStorage / TcpProbe 接口，
        // DataStore 与 java.net.Socket 都不进 commonMain。
        val keyValueStore = DataStoreKeyValueStore(applicationContext)
        val storage = JsonSettingsStorage(keyValueStore)
        // 配对 Token 用 Keystore 里的 AES-GCM 密钥加密后再落 DataStore，磁盘上只有密文。
        val agentTokens = KeystoreAgentTokenStore(keyValueStore)
        // 日志注入而不是写死在 sender 里：`android.util.Log` 在 JVM 单元测试里是 stub，
        // 直接调用会让 androidUnitTest 全红（Task 2 实测）。
        val wakeSender = UdpWakeOnLanSender(logger = { Log.d("WakeOnLan", it) })
        setContent {
            App(
                storage = storage,
                probe = AndroidTcpProbe(),
                wakeSender = wakeSender,
                agentTokens = agentTokens,
            )
        }
    }
}
