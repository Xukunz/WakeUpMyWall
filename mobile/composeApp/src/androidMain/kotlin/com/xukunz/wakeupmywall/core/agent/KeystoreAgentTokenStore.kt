package com.xukunz.wakeupmywall.core.agent

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.xukunz.wakeupmywall.core.storage.KeyValueStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Token 的落盘方式：**Keystore 里的 AES-GCM 密钥加密后再写 KeyValueStore**（DataStore）。
 * 这样磁盘上永远只有密文；Keystore 密钥本身不可导出，设备被 root 也读不出明文 Token。
 *
 * 读取时任何异常（密钥被清、密文损坏、换机恢复）都当作"没配对过"，让用户重新配对，
 * 而不是把异常抛到 UI 上。
 */
class KeystoreAgentTokenStore(private val store: KeyValueStore) : AgentTokenStore {

    override suspend fun read(deviceId: String): String? {
        val encoded = store.read(key(deviceId))
        if (encoded.isNullOrEmpty()) return null
        return runCatching { decrypt(encoded) }.getOrNull()
    }

    override suspend fun write(deviceId: String, token: String) {
        store.write(key(deviceId), encrypt(token))
    }

    override suspend fun clear(deviceId: String) {
        // KeyValueStore 没有 delete：写空串，read 时按"没有 Token"处理。
        store.write(key(deviceId), "")
    }

    private fun key(deviceId: String) = "$Prefix$deviceId"

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(Transformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return "${encode(iv)}:${encode(cipherText)}"
    }

    private fun decrypt(encoded: String): String {
        val (ivPart, textPart) = encoded.split(':', limit = 2)
        val cipher = Cipher.getInstance(Transformation)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, decode(ivPart)))
        return String(cipher.doFinal(decode(textPart)), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(Provider).apply { load(null) }
        (keyStore.getEntry(Alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Provider)
        generator.init(
            KeyGenParameterSpec.Builder(
                Alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val Provider = "AndroidKeyStore"
        const val Alias = "wakeupmywall.agent.token"
        const val Transformation = "AES/GCM/NoPadding"
        const val Prefix = "agent-token:"
    }
}
