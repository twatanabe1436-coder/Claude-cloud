package io.github.twatanabe1436.sodateru.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Claude の API キーの保存先。Android Keystore の鍵で暗号化して保存し、
 * 自動バックアップの対象からも外している (res/xml/data_extraction_rules.xml)。
 */
class SecretStore(context: Context) {
    private val prefs = context.getSharedPreferences("secrets", Context.MODE_PRIVATE)
    private val _hasApiKey = MutableStateFlow(prefs.contains(KEY_API))
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    fun apiKey(): String? {
        val stored = prefs.getString(KEY_API, null) ?: return null
        return runCatching { decrypt(stored) }
            .onFailure { Log.w(TAG, "API key could not be decrypted", it) }
            .getOrNull()
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API, encrypt(key.trim())).apply()
        _hasApiKey.value = true
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API).apply()
        _hasApiKey.value = false
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(byteArrayOf(iv.size.toByte()) + iv + body, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val ivLen = bytes[0].toInt()
        val iv = bytes.copyOfRange(1, 1 + ivLen)
        val body = bytes.copyOfRange(1 + ivLen, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(body), Charsets.UTF_8)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "sodateru_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_API = "claude_api_key"
        const val TAG = "SecretStore"
    }
}
