package de.jepfa.yapm.service.net

import android.content.Context
import android.util.Log
import de.jepfa.yapm.service.secret.SecretService
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.cert.CertificateEncodingException
import java.util.*
import javax.security.auth.x500.X500Principal


object HttpServer {


    private var httpsServer: NettyApplicationEngine? = null
    private var httpServer: NettyApplicationEngine? = null

    fun startWebServerAsync(_port: Int, context: Context): Deferred<Boolean> {

        val inMemoryOTP =
            SecretService.getSecureRandom(context).nextLong().toString() +
            SecretService.getSecureRandom(context).nextLong().toString()

        val certId = SecretService.getSecureRandom(context).nextInt(10000)
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

    fun startApiServerAsync(port: Int, context: Context, callHandler: (String, String) -> Pair<HttpStatusCode, String>): Deferred<Boolean> {
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
                                "X-WebClientId"
                            )

                            call.respond(
                                status = HttpStatusCode.OK,
                                message = ""
                            )
                        }
                        post ("/") {
                            call.response.header(
                                "Access-Control-Allow-Origin",
                                "*"
                            )
                            val webClientId = call.request.headers["X-WebClientId"]
                            if (webClientId == null) {
                                //fail
                                call.respond(HttpStatusCode.BadRequest, "X-WebClientId header missing")
                            }
                            else {
                                val body = call.receive<String>()

                                Log.d("HTTP", "requesting web extension: $webClientId")
                                Log.d("HTTP", "payload: $body")

                                val response = callHandler(webClientId, body)

                                call.respondText(
                                    text = response.second,
                                    contentType = ContentType("text", "json"),
                                    status = response.first
                                )
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


    fun startAllServersAsync(context: Context, callHandler: (String, String) -> Pair<HttpStatusCode, String>): Deferred<Boolean> {

        return CoroutineScope(Dispatchers.IO).async {
            Log.i("HTTP", "ensure shut down")

            val shutdownOk = shutdownAllAsync().await()
            Log.i("HTTP", "shutdownOk=$shutdownOk")

            val startWebServerAsync = startWebServerAsync(8000, context)
            val startApiServerAsync = startApiServerAsync(8001, context, callHandler)

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


    // doesn't work like browser fingerprints ...
    private fun getSHA256Fingerprint(publicKey: PublicKey): String? {
        var hexString: String? = null
        try {


            val tag = "ssh-rsa".toByteArray()
            val pK = publicKey.encoded

            val encoded: ByteArray = tag + pK

            val md = MessageDigest.getInstance("SHA-256")
            val publicKey = md.digest(encoded)
            hexString = byte2HexFormatted(publicKey)
        } catch (e1: NoSuchAlgorithmException) {
            Log.e("HTTP", e1.toString())
        } catch (e: CertificateEncodingException) {
            Log.e("HTTP", e.toString())
        }
        return hexString
    }

    private fun byte2HexFormatted(arr: ByteArray): String {
        val str = StringBuilder(arr.size * 2)
        for (i in arr.indices) {
            var h = Integer.toHexString(arr[i].toInt())
            val l = h.length
            if (l == 1) h = "0${h}"
            if (l > 2) h = h.substring(l - 2, l)
            str.append(h.uppercase(Locale.getDefault()))
            if (i < arr.size - 1) str.append(':')
        }
        return str.toString()
    }


}