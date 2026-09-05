package com.ilinetech.emergency.core.security

import com.ilinetech.emergency.core.model.EstablishmentType
import com.ilinetech.emergency.core.model.FacilitySelection
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Produces the ENCRYPTED_FACILITY_SERIAL used both as the FCM topic suffix
 * and inside the offline SMS payload.
 *
 * Requirements this satisfies:
 *  - Deterministic: the same (wilaya, type, institution, branch, department)
 *    always yields the same serial on any device, with no server round-trip.
 *    This is essential for the offline GSM path, where two phones must agree
 *    on a serial without talking to each other first.
 *  - Compact + SMS-safe: output is Crockford Base32 (uppercase, no padding,
 *    no ambiguous characters), so it survives GSM 7-bit encoding untouched.
 *  - Not human-guessable: the mapping table (wilaya/type/institution/branch
 *    indices) plus AES keeps the plaintext hierarchy out of the SMS payload.
 *
 * Security note: this uses AES/ECB on a single fixed-size block specifically
 * *because* ECB is deterministic (same plaintext -> same ciphertext), which
 * is a requirement here, not an oversight — there is exactly one block, so
 * ECB's usual "patterns repeat across blocks" weakness doesn't apply. If a
 * future requirement drops the "two devices converge offline" constraint,
 * switch to AES/GCM with a server-issued serial instead.
 *
 * The AES key MUST be provisioned via Android Keystore / a build secret,
 * never hardcoded — `keyBytes` below is a placeholder for wiring that up.
 */
object SerialEncoder {

    private const val BLOCK_SIZE = 16 // AES block size in bytes
    private val CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray()

    data class DecodedSerial(
        val wilayaCode: Int,
        val establishmentTypeOrdinal: Int,
        val institutionIndex: Int,
        val branchIndex: Int,
        val departmentOrdinal: Int
    )

    /**
     * Packs the hierarchy indices into a fixed 16-byte block:
     *   [0]     wilaya code        (0-255, wilayas are 1-58)
     *   [1]     establishment type ordinal (0-3)
     *   [2]     institution index within (wilaya, type)  (0-255)
     *   [3]     sub-branch index within institution        (0-255)
     *   [4]     department/role ordinal                    (0-255)
     *   [5..15] reserved / zero-padding (room for future fields, e.g. shift id)
     */
    private fun packBlock(
        wilayaCode: Int,
        type: EstablishmentType,
        institutionIndex: Int,
        branchIndex: Int,
        departmentOrdinal: Int
    ): ByteArray {
        val buf = ByteBuffer.allocate(BLOCK_SIZE)
        buf.put(wilayaCode.toByte())
        buf.put(type.ordinal.toByte())
        buf.put(institutionIndex.toByte())
        buf.put(branchIndex.toByte())
        buf.put(departmentOrdinal.toByte())
        while (buf.position() < BLOCK_SIZE) buf.put(0)
        return buf.array()
    }

    private fun cipher(mode: Int, keyBytes: ByteArray): Cipher {
        require(keyBytes.size == 16 || keyBytes.size == 32) {
            "AES key must be 128 or 256 bits"
        }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(mode, SecretKeySpec(keyBytes, "AES"))
        return cipher
    }

    fun encode(
        selection: FacilitySelection,
        institutionIndex: Int,
        branchIndex: Int,
        departmentOrdinal: Int,
        keyBytes: ByteArray
    ): String = encode(
        wilayaCode = selection.wilaya.code,
        type = selection.type,
        institutionIndex = institutionIndex,
        branchIndex = branchIndex,
        departmentOrdinal = departmentOrdinal,
        keyBytes = keyBytes
    )

    /**
     * Primitive overload used when the caller only has the raw hierarchy
     * indices (e.g. recomputing a sibling department's serial at the same
     * facility from a stored StaffMemberEntity) rather than a full
     * FacilitySelection with resolved Institution/SubBranch objects.
     */
    fun encode(
        wilayaCode: String,
        type: EstablishmentType,
        institutionIndex: Int,
        branchIndex: Int,
        departmentOrdinal: Int,
        keyBytes: ByteArray
    ): String {
        val plain = packBlock(
            wilayaCode = wilayaCode.toInt(),
            type = type,
            institutionIndex = institutionIndex,
            branchIndex = branchIndex,
            departmentOrdinal = departmentOrdinal
        )
        val encrypted = cipher(Cipher.ENCRYPT_MODE, keyBytes).doFinal(plain)
        return base32Encode(encrypted)
    }

    fun decode(serial: String, keyBytes: ByteArray): DecodedSerial {
        val encrypted = base32Decode(serial)
        val plain = cipher(Cipher.DECRYPT_MODE, keyBytes).doFinal(encrypted)
        return DecodedSerial(
            wilayaCode = plain[0].toInt() and 0xFF,
            establishmentTypeOrdinal = plain[1].toInt() and 0xFF,
            institutionIndex = plain[2].toInt() and 0xFF,
            branchIndex = plain[3].toInt() and 0xFF,
            departmentOrdinal = plain[4].toInt() and 0xFF
        )
    }

    // --- Crockford Base32, no padding, SMS/GSM-7 safe ---

    private fun base32Encode(data: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                val index = (buffer shr bitsLeft) and 0x1F
                sb.append(CROCKFORD_ALPHABET[index])
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            sb.append(CROCKFORD_ALPHABET[index])
        }
        return sb.toString()
    }

    private fun base32Decode(input: String): ByteArray {
        val cleaned = input.uppercase().filter { it != '-' }
        var buffer = 0
        var bitsLeft = 0
        val out = ArrayList<Byte>()
        for (c in cleaned) {
            val value = CROCKFORD_ALPHABET.indexOf(c)
            require(value >= 0) { "Invalid Crockford Base32 character: $c" }
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }
}
