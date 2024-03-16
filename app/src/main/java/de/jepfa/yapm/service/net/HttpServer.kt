package de.jepfa.yapm.service.net

import android.content.Context
import android.util.Base64
import android.util.Log
import de.jepfa.yapm.model.Validable.Companion.FAILED_STRING
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.sha256
import de.jepfa.yapm.util.toHex
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.cert.CertificateEncodingException
import java.util.*
import javax.security.auth.x500.X500Principal


object HttpServer {


    enum class Action {LINKING, REQUEST_CREDENTIAL}

    interface Listener {
        fun callHttpRequestHandler(action: Action, webClientId: String, webExtension: EncWebExtension, message: JSONObject): Pair<HttpStatusCode, JSONObject>
    }

    private var httpsServer: NettyApplicationEngine? = null
    private var httpServer: NettyApplicationEngine? = null

    var linkListener: Listener? = null
    var requestCredentialListener: Listener? = null

    fun startWebServerAsync(_port: Int, activity: SecureActivity): Deferred<Boolean> {

        val inMemoryOTP =
            SecretService.getSecureRandom(activity).nextLong().toString() +
            SecretService.getSecureRandom(activity).nextLong().toString()

        val certId = SecretService.getSecureRandom(activity).nextInt(10000)
        return CoroutineScope(Dispatchers.IO).async {
            Log.i("HTTP", "start TLS server")
            try {
                val alias = "anotherpass_https_cert"
                val keyStore = buildKeyStore {
                    certificate(alias) {
                        daysValid = 365
                        subject = X500Principal("CN=ID$certId, OU=ANOTHERpass, O=jepfa, C=DE")
                        password = inMemoryOTP
                    }
                }

                val publicKey = keyStore.getCertificate(alias).publicKey
                val fingerprint = getSHA256Fingerprint(publicKey)

                val environment = applicationEngineEnvironment {
                    log = LoggerFactory.getLogger("ktor.application")
                    sslConnector(
                        keyStore = keyStore,
                        keyAlias = alias,
                        keyStorePassword = { CharArray(0) },
                        privateKeyPassword = { inMemoryOTP.toCharArray() }
                    )
                    {
                        port = _port
                    }
                    module {
                        routing {
                            get("/") {
                                call.response.header(
                                    "Access-Control-Allow-Origin",
                                    "*"
                                )
                                call.respondText(
                                    text = "<h1>Hello, this is ANOTHERpass on TLS! Current certificate id is $certId and fingerprint is <pre>$fingerprint</pre></h1>",
                                    contentType = ContentType("text", "html"),
                                )
                            }
                        }
                    }
                }

                httpsServer = embeddedServer(Netty, environment)
                Log.i("HTTP", "launch Web server")
                httpsServer?.start(wait = false)
                Log.i("HTTP", "TLS server started")
                true
            } catch (e: Exception) {
                Log.e("HTTP", e.toString())
                false
            }
        }
    }

    fun startApiServerAsync(port: Int, activity: SecureActivity,
                            pingHandler: (String) -> Unit,
    ): Deferred<Boolean> {
        return CoroutineScope(Dispatchers.IO).async {

            Log.i("HTTP", "start API server")
            try {
                httpServer = embeddedServer(Netty, port = port) {
                    routing {
                        get ("/") {

                            call.response.header(
                                "Access-Control-Allow-Origin",
                                "*"
                            )
                            call.respondText(
                                text = "Use the ANOTHERpass browser extension to use the app as a credential server.",
                                contentType = ContentType("text", "html"),
                            )
                        }
                        options {
                            call.response.header(
                                "Access-Control-Allow-Origin",
                                "*"
                            )
                            call.response.header(
                                "Access-Control-Allow-Headers",
                                "X-WebClientId,Content-Type"
                            )

                            call.respond(
                                status = HttpStatusCode.OK,
                                message = ""
                            )
                        }
                        post ("/") {
                            try {
                                call.response.header(
                                    "Access-Control-Allow-Origin",
                                    "*"
                                )

                                val webClientId = call.request.headers["X-WebClientId"]
                                pingHandler(webClientId?:"Unknown")

                                if (webClientId == null) {
                                    //fail
                                    respondError(HttpStatusCode.BadRequest, "X-WebClientId header missing")
                                    return@post
                                }

                                val key = activity.masterSecretKey
                                if (key == null) {
                                    respondError(HttpStatusCode.Unauthorized, "Locked")
                                    return@post
                                }

                                val webExtension = activity.getApp().webExtensionRepository.getAllSync()
                                    .find { SecretService.decryptCommonString(key, it.webClientId) == webClientId }
                                if (webExtension == null) {
                                    respondError(HttpStatusCode.NotFound, "$webClientId is unknown")
                                    return@post
                                }
                                Log.d("HTTP", "checking WebExtensionId: ${webExtension.id}")

                                if (!webExtension.enabled) {
                                    respondError(HttpStatusCode.Forbidden, "$webClientId is blocked")
                                    return@post
                                }
                                Log.d("HTTP", "handling WebExtensionId: ${webExtension.id}")

                                val body = call.receive<String>()

                                Log.d("HTTP", "requesting web extension: $webClientId")
                                Log.d("HTTP", "payload: $body")

                                val message = unwrapBody(key, webExtension, webClientId, JSONObject(body), activity)
                                if (message == null) {
                                    respondError(HttpStatusCode.BadRequest, "Cannot parse message")
                                    return@post
                                }

                                val response = handleAction(webExtension, webClientId, message)
                                if (response == null) {
                                    respondError(HttpStatusCode.InternalServerError, "Missing action listener")
                                    return@post
                                }

                                val text = wrapBody(webClientId, response.second)

                                respond(text, response)
                            } catch (e: Exception) {
                                Log.e("HTTP", "Something went wrong!!!", e)
                                respondError(HttpStatusCode.InternalServerError, e.message?:e.toString())
                                return@post
                            }
                        }
                    }
                }

                Log.i("HTTP", "launch API server")
                httpServer?.start(wait = false)
                Log.i("HTTP", "API server started")
                true
            } catch (e: Exception) {
                Log.e("HTTP", e.toString())
                false
            }
        }
    }

    fun startAllServersAsync(
        activity: SecureActivity,
        pingHandler: (String) -> Unit,
    ): Deferred<Boolean> {

        return CoroutineScope(Dispatchers.IO).async {
            Log.i("HTTP", "ensure shut down")

            val shutdownOk = shutdownAllAsync().await()
            Log.i("HTTP", "shutdownOk=$shutdownOk")

            val startWebServerAsync = startWebServerAsync(8000, activity)
            val startApiServerAsync = startApiServerAsync(8001, activity, pingHandler)

            Log.i("HTTP", "awaiting start")

            val successList = awaitAll(startWebServerAsync, startWebServerAsync)
            val successWebServer = successList.first()
            val successApiServer = successList.last()
            Log.i("HTTP", "successWebServer = $successWebServer")
            Log.i("HTTP", "successApiServer = $successApiServer")

            successWebServer && successApiServer
        }

    }

    fun shutdownAllAsync() : Deferred<Boolean> {
        linkListener = null
        requestCredentialListener = null
        return CoroutineScope(Dispatchers.IO).async {
            try {
                Log.i("HTTP", "shutdown all")

                httpsServer?.stop()
                httpServer?.stop()
                Log.i("HTTP", "shutdown done")

                true
            } catch (e: Exception) {
                Log.e("HTTP", e.toString())
                false
            }
        }
    }

    private fun unwrapBody(key: SecretKeyHolder, webExtension: EncWebExtension, webClientId: String, body: JSONObject, context: Context): JSONObject? {
        val isLinking = !webExtension.linked
        if (isLinking) {
            val sessionKeyAndPubKeyFingerprint = SecretService.decryptCommonString(key, webExtension.extensionPublicKey)

            val splitted = sessionKeyAndPubKeyFingerprint.split(":")
            if (splitted.size != 2) {
                Log.w("HTTP", "Invalid stored link data")
                return null
            }
            val sessionKeyBase64 = splitted[0]
            Log.d("HTTP", "using sessionKeyBase64=$sessionKeyBase64")

            val sessionKey = Key(Base64.decode(sessionKeyBase64, Base64.DEFAULT))
            if (!sessionKey.isValid()) {
                Log.w("HTTP", "Invalid session key")
                return null
            }
            // use AES to decrypt the encrypted body
            val envelope = body.getString("envelope")

            Log.d("HTTP", "envelope=$envelope")
            val encEnvelope = Encrypted.fromBase64String(envelope)

            // decrypt envelope with AES key 'secret'
            Log.d("HTTP", "sessionKey.array=${sessionKey.debugToString()}")
            Log.d("HTTP", "sessionKey.length=${sessionKey.data.size}")

            val secretKey = SecretService.generateAesKey(sessionKey, context)
            val decryptedEnvelope = SecretService.decryptCommonString(secretKey, encEnvelope)
            if (decryptedEnvelope == FAILED_STRING) {
                Log.w("HTTP", "Invalid envelope")
                return null
            }
            return JSONObject(decryptedEnvelope)
        }
        else {
            val clientPublicKey = SecretService.decryptKey(key, webExtension.extensionPublicKey)

            // use server private key to decrypt session key and then use that to decrypt body
            //TODO encrypt body with either the temporary session key or the server private key
            return JSONObject()
        }
    }


    private fun wrapBody(webClientId: String, message: JSONObject): String {
        //TODO decrypt body with either the temporary session key or the server private key
        return message.toString()
    }

    private fun toErrorJson(msg: String): JSONObject {
        val errorJson = JSONObject()
        errorJson.put("error", msg)
        return errorJson
    }


    private fun handleAction(
        webExtension: EncWebExtension,
        webClientId: String,
        message: JSONObject,
    ): Pair<HttpStatusCode, JSONObject>? {
        return when (val action = message.get("action")) {
            "link_app" -> handleLinking(Action.LINKING, webClientId, webExtension, message)
            "request_credential" -> handleRequestCredential(Action.REQUEST_CREDENTIAL, webClientId, webExtension, message)
            else -> Pair(HttpStatusCode.BadRequest, toErrorJson("unknown action: $action"))
        }
    }

    private fun handleLinking(
        action: Action,
        webClientId: String,
        webExtension: EncWebExtension,
        message: JSONObject,
    ): Pair<HttpStatusCode, JSONObject>? {
        Log.d("HTTP", "linking ...")
        return linkListener?.callHttpRequestHandler(action, webClientId, webExtension, message)
    }

    private fun handleRequestCredential(
        action: Action,
        webClientId: String,
        webExtension: EncWebExtension,
        message: JSONObject,
    ): Pair<HttpStatusCode, JSONObject>? {
        Log.d("HTTP", "linking ...")
        return requestCredentialListener?.callHttpRequestHandler(action, webClientId, webExtension, message)
    }

    // doesn't work like browser fingerprints ...
    private fun getSHA256Fingerprint(publicKey: PublicKey): String? {
        var hexString: String? = null
        try {


            val tag = "ssh-rsa".toByteArray()
            val pK = publicKey.encoded

            val encoded: ByteArray = tag + pK

            hexString = encoded.sha256().toHex()
        } catch (e1: NoSuchAlgorithmException) {
            Log.e("HTTP", e1.toString())
        } catch (e: CertificateEncodingException) {
            Log.e("HTTP", e.toString())
        }
        return hexString
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.respond(
        text: String,
        response: Pair<HttpStatusCode, JSONObject>
    ) {
        call.respondText(
            text = text,
            contentType = ContentType("application", "json"),
            status = response.first
        )
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.respondError(
        code: HttpStatusCode,
        msg: String
    ) {
        call.respondText(
            text = toErrorJson(msg).toString(4),
            contentType = ContentType("application", "json"),
            status = code
        )
    }

}