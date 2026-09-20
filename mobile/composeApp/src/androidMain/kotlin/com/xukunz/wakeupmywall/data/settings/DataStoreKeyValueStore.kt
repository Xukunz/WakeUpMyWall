package com.xukunz.wakeupmywall.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xukunz.wakeupmywall.core.storage.KeyValueStore
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore Preferences 只是"键 → 字符串"的容器；设备与外观怎么序列化由 commonMain 的
 * `JsonSettingsStorage` 决定，保证序列化 schema 与平台无关、可被单测覆盖。
 */
class DataStoreKeyValueStore(private val context: Context) : KeyValueStore {

    override suspend fun read(key: String): String? =
        context.settingsDataStore.data.first()[stringPreferencesKey(key)]

    override suspend fun write(key: String, value: String) {
        context.settingsDataStore.edit { it[stringPreferencesKey(key)] = value }
    }
}
