package com.ilinetech.emergency.core.security

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * IMPORTANT — read before touching this file.
 *
 * SerialEncoder needs the SAME AES key on every device, because a sender's
 * phone and a receiver's phone must independently arrive at the identical
 * encrypted serial for the same (wilaya, type, institution, branch) with no
 * server round-trip (that's the whole point of the offline SMS path — see
 * SerialEncoder's class doc). That means this can NOT be a per-device
 * Android Keystore key (Keystore keys are non-exportable and unique per
 * device by design) — it must be a shared application-level secret baked
 * into every install of the app.
 *
 * That has real consequences:
 *  - This key must never be hardcoded as a literal in source that's pushed
 *    to a public (or even shared-private) repo. Anyone who reads it can
 *    decrypt every facility/department serial ever generated.
 *  - Store it via a Gradle property injected at build time
 *    (`local.properties`, which is already gitignored by the standard
 *    Android template) exposed through BuildConfig, e.g.:
 *
 *      // local.properties (NOT committed):
 *      ILINE_SERIAL_KEY_HEX=00112233445566778899aabbccddeeff
 *
 *      // app/build.gradle:
 *      buildConfigField "String", "SERIAL_KEY_HEX", "\"${project.findProperty('ILINE_SERIAL_KEY_HEX') ?: ''}\""
 *
 *  - Rotating this key requires an app update pushed to every device before
 *    the rotation date — there is no migration path for offline devices
 *    that haven't updated yet, since they'd compute different serials than
 *    devices that have. Plan key rotation around release cadence, not
 *    on-demand.
 *  - CI/CD (your GitHub Actions Windows-runner build) needs this value
 *    injected as a repository secret, not committed alongside the workflow
 *    file.
 *
 * The placeholder below is intentionally obviously-wrong (all zero bytes)
 * so a build using it is easy to spot in testing and impossible to mistake
 * for a real production key.
 */
object AppKeyProvider {

    private val PLACEHOLDER_KEY = ByteArray(16) // 128-bit all-zero — DEV ONLY

    fun getSerialEncryptionKey(): ByteArray {
        // TODO: read from BuildConfig.SERIAL_KEY_HEX once the Gradle wiring
        // above is in place, e.g.:
        //   return BuildConfig.SERIAL_KEY_HEX.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return PLACEHOLDER_KEY
    }
}
