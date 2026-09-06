/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Deployable Firebase Cloud Function — the trusted-server piece the FCM
 * client SDK cannot replace (client apps can only subscribe/receive, never
 * publish, to a topic; see RemoteAlertPublisher.kt on the Android side).
 *
 * Deploy with Firebase CLI (see ../CLOUD_FUNCTION_NOTES.md for full steps):
 *   firebase deploy --only functions:publishAlert
 *
 * SECURITY: this endpoint can push a notification to any topic it's told
 * to — without the shared-secret check below, anyone who discovers the URL
 * could spam every registered device. Set SHARED_SECRET via
 *   firebase functions:secrets:set SHARED_SECRET
 * and it must match CLOUD_FUNCTION_SHARED_SECRET in the Android app's
 * local.properties / CI secret (same shared-key pattern already used for
 * ILINE_SERIAL_KEY_HEX).
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const SHARED_SECRET = functions.config().app?.shared_secret || process.env.SHARED_SECRET || "";

/** Must exactly match FcmTopicManager.topicNameFor() on the Android side. */
function topicNameFor(facilitySerial, deptSerial) {
  return `fac_${facilitySerial}_${deptSerial}`;
}

exports.publishAlert = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed" });
    return;
  }

  if (SHARED_SECRET && req.get("X-Shared-Secret") !== SHARED_SECRET) {
    res.status(401).json({ error: "Invalid or missing shared secret" });
    return;
  }

  const { facilitySerial, deptSerial, groupId, priority, reason, message, senderLabel } = req.body || {};

  if (!facilitySerial || !deptSerial) {
    res.status(400).json({ error: "facilitySerial and deptSerial are required" });
    return;
  }

  const topic = topicNameFor(facilitySerial, deptSerial);

  try {
    const messageId = await admin.messaging().send({
      topic,
      // FCM data messages require all values to be strings.
      data: {
        facilitySerial: String(facilitySerial),
        deptSerial: String(deptSerial),
        groupId: String(groupId || ""),
        priority: String(priority ?? 2),
        reason: String(reason || "AUTRE"),
        message: String(message || ""),
        senderLabel: String(senderLabel || "Alerte"),
      },
      android: {
        priority: "high",
      },
    });

    res.status(200).json({ messageId });
  } catch (err) {
    console.error("FCM publish failed", err);
    res.status(500).json({ error: err.message || "FCM publish failed" });
  }
});
