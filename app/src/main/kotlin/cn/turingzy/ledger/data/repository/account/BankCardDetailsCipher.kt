package cn.turingzy.ledger.data.repository.account

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import cn.turingzy.ledger.model.account.BankCardDetails
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/** 使用本机 Android Keystore 密钥保护钱包中的银行卡信息。 */
@Singleton
class BankCardDetailsCipher @Inject constructor() {
    private val keyStore by lazy { KeyStore.getInstance("AndroidKeyStore").apply { load(null) } }

    /** 每次保存使用由 Android Keystore 生成的新 IV。 */
    fun encrypt(details: BankCardDetails): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val data = cipher.doFinal(Json.encodeToString(details).toByteArray(Charsets.UTF_8))
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(cipher.iv) + "." + encoder.encodeToString(data)
    }

    /** 解密时沿用设备上的密钥，密钥缺失时不创建替代密钥。 */
    fun decrypt(value: String): BankCardDetails {
        val (iv, data) = value.split('.', limit = 2)
        val decoder = Base64.getDecoder()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = requireNotNull(keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, decoder.decode(iv)))
        return Json.decodeFromString(cipher.doFinal(decoder.decode(data)).toString(Charsets.UTF_8))
    }

    /** 串行创建密钥，避免并发首次保存时覆盖已有密钥。 */
    @Synchronized
    private fun encryptionKey(): SecretKey = (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
        ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()

    private companion object {
        const val KEY_ALIAS = "ledger_wallet_bank_cards"
    }
}
