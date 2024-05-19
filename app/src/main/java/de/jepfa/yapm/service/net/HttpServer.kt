package de.jepfa.yapm.service.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.model.Validable.Companion.FAILED_STRING
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.*
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.net.InetAddress
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.cert.CertificateEncodingException
import java.util.*
import javax.security.auth.x500.X500Principal


object HttpServer {

    val DEFAULT_HTTP_SERVER_PORT = 8001
    val SERVER_LOG_PREFIX = Constants.LOG_PREFIX + "HttpServer"

    enum class Action {LINKING, REQUEST_CREDENTIAL}

    interface HttpServerCallback {
        fun handleOnWifiEstablished()
        fun handleOnWifiUnavailable()
        fun handleOnIncomingRequest(webClientId: String?)
    }

    interface HttpCallback {
        fun handleHttpRequest(action: Action, webClientId: String, webExtension: EncWebExtension, message: JSONObject): Pair<HttpStatusCode, JSONObject>
    }


    private var httpServer: NettyApplicationEngine? = null
    private var isHttpServerRunning = false

    var linkHttpCallback: HttpCallback? = null
    var requestCredentialHttpCallback: HttpCallback? = null

    fun startApiServerAsync(
        _port: Int, activity: SecureActivity,
        httpServerCallback: HttpServerCallback,
    ): Deferred<Boolean> {
        return CoroutineScope(Dispatchers.IO).async {

            Log.i("HTTP", "start API server")
            try {
                val environment = applicationEngineEnvironment {
                    connector {
                        port = _port
                    }
                    module {
                        routing {
                            get("/") {

                                call.response.header(
                                    "Access-Control-Allow-Origin",
                                    "*"
                                )

                                serverLog(
                                    origin = call.request.origin,
                                    msg = "GET received",
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

                                serverLog(origin = call.request.origin, msg = "OPTIONS received")

                                call.respond(
                                    status = HttpStatusCode.OK,
                                    message = ""
                                )
                            }
                            post("/") {
                                var webClientId: String? = null
                                try {
                                    call.response.header(
                                        "Access-Control-Allow-Origin",
                                        "*"
                                    )

                                    serverLog(
                                        origin = call.request.origin,
                                        msg = "POST received",
                                    )


                                    webClientId = call.request.headers["X-WebClientId"]
                                    httpServerCallback.handleOnIncomingRequest(webClientId)

                                    if (webClientId == null) {
                                        //fail
                                        respondError(
                                            null,
                                            HttpStatusCode.BadRequest,
                                            "X-WebClientId header missing",
                                        )
                                        return@post
                                    }

                                    val key = activity.masterSecretKey
                                    if (key == null) {
                                        respondError(
                                            webClientId,
                                            HttpStatusCode.Unauthorized,
                                            "Locked",
                                        )
                                        return@post
                                    }

                                    val webExtension =
                                        activity.getApp().webExtensionRepository.getAllSync()
                                            .find {
                                                SecretService.decryptCommonString(
                                                    key,
                                                    it.webClientId
                                                ) == webClientId
                                            }
                                    if (webExtension == null) {
                                        respondError(
                                            webClientId,
                                            HttpStatusCode.NotFound,
                                            "webClientId is unknown",
                                        )
                                        return@post
                                    }
                                    Log.d("HTTP", "checking WebExtensionId: ${webExtension.id}")

                                    if (!webExtension.enabled) {
                                        respondError(
                                            webClientId,
                                            HttpStatusCode.Forbidden,
                                            "webClientId is blocked",
                                        )
                                        return@post
                                    }
                                    Log.d("HTTP", "handling WebExtensionId: ${webExtension.id}")

                                    val body = call.receive<String>()

                                    Log.d("HTTP", "requesting web extension: $webClientId")
                                    Log.d("HTTP", "payload: $body")

                                    val sharedBaseOrLinkingSessionKey = SecretService.decryptKey(key, webExtension.sharedBaseKey)
                                    if (!sharedBaseOrLinkingSessionKey.isValid()) {
                                        Log.w("HTTP", "No configured base key")
                                        respondError(
                                            webClientId,
                                            HttpStatusCode.BadRequest,
                                            "No base key",
                                        )
                                        return@post
                                    }

                                    val payload = JSONObject(body)


                                    val requestTransportKey = extractRequestTransportKey(
                                        sharedBaseOrLinkingSessionKey,
                                        webExtension,
                                        payload.optString("encOneTimeKey")
                                    )
                                    if (requestTransportKey == null) {
                                        respondError(
                                            webClientId,
                                            HttpStatusCode.BadRequest,
                                            "Cannot derive request transport key",
                                        )
                                        return@post
                                    }

                                    val message = unwrapBody(
                                        requestTransportKey,
                                        payload,
                                        activity
                                    )
                                    if (message == null) {
                                        respondError(
                                            webClientId,
                                            HttpStatusCode.BadRequest,
                                            "Cannot parse message",
                                        )
                                        return@post
                                    }

                                    val response = handleAction(call.request.origin, webExtension, webClientId, message)
                                    if (response == null) {
                                        respondError(
                                            webClientId,
                                            HttpStatusCode.InternalServerError,
                                            "Missing action listener",
                                        )
                                        return@post
                                    }

                                    if (!response.first.isSuccess()) {
                                        respondError(
                                            webClientId,
                                            response.first,
                                            response.second,
                                        )
                                        return@post
                                    }

                                    val responseKeys = extractResponseTransportKey(sharedBaseOrLinkingSessionKey, key, webExtension, activity)
                                    if (responseKeys == null) {
                                        respondError(
                                            webClientId,
                                            HttpStatusCode.BadRequest,
                                            "Cannot provide a valid response",
                                        )
                                        return@post
                                    }

                                    val text = wrapBody(
                                        responseKeys,
                                        key,
                                        webExtension,
                                        response.second,
                                        activity)

                                    respond(webClientId, text, response)
                                } catch (e: Exception) {
                                    Log.e("HTTP", "Something went wrong!!!", e)
                                    respondError(
                                        webClientId,
                                        HttpStatusCode.InternalServerError,
                                        "Unexpected error during handling post request: ${e.message ?: e.toString()}",
                                    )
                                    return@post
                                }
                            }
                        }
                    }
                }

                Log.i("HTTP", "launch API server")
                if (httpServer != null) {
                    httpServer?.stop()
                }
                httpServer = embeddedServer(Netty, environment)
                httpServer?.start(wait = false)
                Log.i("HTTP", "API server started")

                isHttpServerRunning = true
                true
            } catch (e: Exception) {
                Log.e("HTTP", e.toString())
                isHttpServerRunning = false
                false
            }
        }
    }

    fun startAllServersAsync(
        activity: SecureActivity,
        httpServerCallback: HttpServerCallback,
    ): Deferred<Boolean> {

        return CoroutineScope(Dispatchers.IO).async {
            Log.i("HTTP", "ensure shut down")

            val shutdownOk = shutdownAllAsync().await()
            Log.i("HTTP", "shutdownOk=$shutdownOk")

            if (!isWifiEnabled(activity)) {
                Log.w("HTTP", "Wifi not enabled")
                return@async false
            }


            var httpServerPort = PreferenceService.getAsInt(PreferenceService.PREF_SERVER_PORT, activity)
            if (httpServerPort <= 0) httpServerPort = DEFAULT_HTTP_SERVER_PORT
            val startApiServerAsync = startApiServerAsync(httpServerPort, activity, httpServerCallback)
            Log.i("HTTP", "awaiting start")

            val (successApiServer) = awaitAll(startApiServerAsync)
            Log.i("HTTP", "successApiServer = $successApiServer")

            monitorWifiEnablement(activity, httpServerCallback)

            val success = successApiServer
            if (success) {
                serverLog(
                    webClientId = null,
                    msg = "Server started on port $httpServerPort",
                )
            }

            return@async success
        }

    }

    fun isWifiEnabled(activity: SecureActivity): Boolean {
        val wifi = activity.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifi.isWifiEnabled
    }

    private fun monitorWifiEnablement(
        activity: SecureActivity,
        httpServerCallback: HttpServerCallback
    ) {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED) // with trusted, we exclude unknown wifi AP
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // network is available for use
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (isRunning()) {
                    httpServerCallback.handleOnWifiEstablished()
                }
            }

            // Network capabilities have changed for the network
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val unmetered =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            }

            // lost network connection
            override fun onLost(network: Network) {
                super.onLost(network)
                if (isRunning()) {
                    httpServerCallback.handleOnWifiUnavailable()
                }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                if (isRunning()) {
                    httpServerCallback.handleOnWifiUnavailable()
                }
            }
        }


        val connectivityManager =
            activity.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    fun isRunning(): Boolean {
        return isHttpServerRunning
    }

    fun shutdownAllAsync() : Deferred<Boolean> {
        linkHttpCallback = null
        requestCredentialHttpCallback = null
        return CoroutineScope(Dispatchers.IO).async {
            try {
                Log.i("HTTP", "shutdown all")

                httpServer?.stop()
                isHttpServerRunning = false
                Log.i("HTTP", "shutdown done")

                if (httpServer != null) { //server ran before
                    serverLog(
                        webClientId = null,
                        msg = "Server stopped",
                    )
                }
                httpServer = null

                true
            } catch (e: Exception) {
                Log.e("HTTP", e.toString())
                false
            }
        }
    }

    private fun serverLog(
        origin: RequestConnectionPoint? = null,
        webClientId: String? = null,
        msg: String,
    ) {
        val dateTime = Date().toSimpleDateTimeFormat()
        if (webClientId == null && origin == null) {
            Log.i(SERVER_LOG_PREFIX, "$dateTime : $msg")
        }
        else {
            val webClient = webClientId ?: "-unknwn-"
            if (origin != null) {
                val remoteHost = origin.remoteHost
                val remotePort = origin.remotePort
                Log.i(SERVER_LOG_PREFIX, "$dateTime [ $webClient @ $remoteHost:$remotePort ] : $msg")
            }
            else {
                Log.i(SERVER_LOG_PREFIX, "$dateTime [$webClient] : $msg")
            }
        }
    }

    private fun extractRequestTransportKey(sharedBaseKey: Key, webExtension: EncWebExtension, encOneTimeKeyBase64: String): Key? {

        val isLinking = !webExtension.linked
        if (isLinking) {
            // During linking phase the stored sharedBaseKey contains the linking session key(previously scanned from QR code)
            return sharedBaseKey
        }
        else {
            val encOneTimeKey = Base64.decode(encOneTimeKeyBase64, Base64.DEFAULT)
            val serverPrivateKey = SecretService.getServerPrivateKey(webExtension.getServerKeyPairAlias()) ?: return null
            Log.d("HTTP", "encOneTimeKey=" + encOneTimeKey.contentToString())

            val decOneTimeKey = SecretService.decryptKeyWithPrivateKey(serverPrivateKey, encOneTimeKey, workaroundMode = true)
            Log.d("HTTP", "sharedBaseKeyBase64=" + sharedBaseKey.toBase64String())
            Log.d("HTTP", "reqOneTimeKeyBase64=" + decOneTimeKey.toBase64String())

            return SecretService.conjunctKeys(sharedBaseKey, decOneTimeKey)
        }
    }

    /**
     * Returns first the responseTransportKey second the one-time key as base64
     */
    private fun extractResponseTransportKey(linkingSessionKey: Key, key: SecretKeyHolder, webExtension: EncWebExtension, context: Context): Pair<Key, Key>? {
        val oneTimeKey = SecretService.generateRandomKey(16, context)

        val isLinking = !webExtension.linked
        val responseTransportKey = if (isLinking) {
            // During linking sharedBaseKey contains the previously scanned session key
            SecretService.conjunctKeys(linkingSessionKey, oneTimeKey)
        }
        else {
            val sharedBaseKey = SecretService.decryptKey(key, webExtension.sharedBaseKey)
            if (!sharedBaseKey.isValid()) {
                Log.w("HTTP", "No configured base key")
                return null
            }
            SecretService.conjunctKeys(sharedBaseKey, oneTimeKey)
        }

        return Pair(responseTransportKey, oneTimeKey)
    }

    private fun unwrapBody(transportKey: Key, body: JSONObject, context: Context): JSONObject? {
        val envelope = body.getString("envelope")
        Log.d("HTTP", "envelope=$envelope")
        val encEnvelope = Encrypted.fromBase64String(envelope)

        Log.d("HTTP", "reqTransportKey.array=${transportKey.debugToString()}")
        Log.d("HTTP", "reqTransportKey.length=${transportKey.data.size}")
        Log.d("HTTP", "reqTransportKey.base64=${transportKey.toBase64String()}")

        val secretKey = SecretService.buildAesKey(transportKey, context)
        val decryptedEnvelope = SecretService.decryptCommonString(secretKey, encEnvelope)
        if (decryptedEnvelope == FAILED_STRING) {
            Log.w("HTTP", "Invalid envelope")
            return null
        }
        val jsonBody = JSONObject(decryptedEnvelope)
        Log.d("HTTP","unwrapped request: " + jsonBody.toString(4))

        return jsonBody
    }

    fun getHostNameOrIp(context: Context, getHostNameCallback: (String) -> Unit): String {
        val wifiManager = context.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
        val ipAddress =
            Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        getHostName(ipAddress) { hostName ->
            CoroutineScope(Dispatchers.Main).launch {
                if (hostName != null && hostName != ipAddress) {
                    getHostNameCallback("${hostName.lowercase()} ($ipAddress)")
                } else {
                    getHostNameCallback("$ipAddress")
                }
            }
        }

        return ipAddress
    }

    private fun getHostName(ipAddress: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hostName = withContext(Dispatchers.IO) {
                    InetAddress.getByName(ipAddress)
                }.hostName
                callback(hostName)
            } catch (e: Exception) {
                Log.w("HTTP", "Cannot get host name from IP:", e)
                callback(null)
            }
        }
    }

    private fun wrapBody(responseKeys: Pair<Key, Key>, key: SecretKeyHolder, webExtension: EncWebExtension, message: JSONObject, context: Context): String {
        val responseTransportKey = responseKeys.first
        val oneTimeKey = responseKeys.second
        val secretKey = SecretService.buildAesKey(responseTransportKey, context)
        val encryptedEnvelope = SecretService.encryptCommonString(EncryptedType(EncryptedType.Types.ENC_WEB_MESSAGE), secretKey, message.toString())
        responseTransportKey.clear()

        val clientPubKeyAsJWK = JSONObject(SecretService.decryptCommonString(key, webExtension.extensionPublicKey))

        val nBase64 = clientPubKeyAsJWK.getString("n")
        val eBase64 = clientPubKeyAsJWK.getString("e")
        val nBytes = Base64.decode(nBase64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val eBytes = Base64.decode(eBase64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

        val modulus = BigInteger(1, nBytes)
        val exponent = BigInteger(1, eBytes)
        Log.d("HTTP","client nB.s: " + nBytes.size)
        Log.d("HTTP","client nB: " + nBytes.contentToString())
        Log.d("HTTP","client modulus: " + modulus)
        Log.d("HTTP","client exponent: " + exponent)

        val clientPublicKey = SecretService.buildRsaPublicKey(modulus, exponent)
        val encOneTimeKey = SecretService.encryptKeyWithPublicKey(clientPublicKey, oneTimeKey)
        val encOneTimeKeyBase64 = Base64.encodeToString(encOneTimeKey, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

        val envelope = JSONObject()
        envelope.put("encOneTimeKey", encOneTimeKeyBase64)
        envelope.put("envelope", encryptedEnvelope.toBase64String())
        Log.d("HTTP","wrapped response: " + envelope.toString(4))

        oneTimeKey.clear()
        responseTransportKey.clear()
        secretKey.destroy()

        return envelope.toString()
    }

    private fun toErrorJson(msg: String): JSONObject {
        val errorJson = JSONObject()
        errorJson.put("error", msg)
        return errorJson
    }

    fun toErrorResponse(httStatusCode: HttpStatusCode, msg: String): Pair<HttpStatusCode, JSONObject> =
        Pair(httStatusCode, toErrorJson(msg))

    private fun handleAction(
        origin: RequestConnectionPoint,
        webExtension: EncWebExtension,
        webClientId: String,
        message: JSONObject,
    ): Pair<HttpStatusCode, JSONObject>? {

        val action = message.get("action")

        serverLog(
            origin = origin,
            webClientId = webClientId,
            msg = "Handling action $action",
        )

        return when (action) {
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
        return linkHttpCallback?.handleHttpRequest(action, webClientId, webExtension, message)
    }

    private fun handleRequestCredential(
        action: Action,
        webClientId: String,
        webExtension: EncWebExtension,
        message: JSONObject,
    ): Pair<HttpStatusCode, JSONObject>? {
        Log.d("HTTP", "credential request ...")
        return requestCredentialHttpCallback?.handleHttpRequest(action, webClientId, webExtension, message)
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
        webClientId: String,
        text: String,
        response: Pair<HttpStatusCode, JSONObject>
    ) {

        serverLog(
            origin = call.request.origin,
            webClientId = webClientId,
            msg = "Respond OK",
        )

        call.respondText(
            text = text,
            contentType = ContentType("application", "json"),
            status = response.first
        )
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.respondError(
        webClientId: String?,
        code: HttpStatusCode,
        json: JSONObject,
    ) {

        serverLog(
            origin = call.request.origin,
            webClientId = webClientId,
            msg = "Error response: $code - ${json.optString("error")} ",
        )

        call.respondText(
            text = json.toString(4),
            contentType = ContentType("application", "json"),
            status = code
        )
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.respondError(
        webClientId: String?,
        code: HttpStatusCode,
        msg: String,
    ) {
        serverLog(
            origin = call.request.origin,
            webClientId = webClientId,
            msg = "Error response: $code - $msg",
        )

        call.respondText(
            text = toErrorJson(msg).toString(4),
            contentType = ContentType("application", "json"),
            status = code
        )
    }

}