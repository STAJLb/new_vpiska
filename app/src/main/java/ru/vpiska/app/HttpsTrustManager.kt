package ru.vpiska.app

import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Created by Кирилл on 05.02.2018.
 */

class HttpsTrustManager : X509TrustManager {

    override fun checkClientTrusted(
            x509Certificates: Array<java.security.cert.X509Certificate>, s: String) {

    }

    override fun checkServerTrusted(
            x509Certificates: Array<java.security.cert.X509Certificate>, s: String) {

    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return _AcceptedIssuers
    }

    companion object {

        private var trustManagers: Array<TrustManager>? = null
        private val _AcceptedIssuers = arrayOf<X509Certificate>()

        fun allowAllSSL() {
            HttpsURLConnection.setDefaultHostnameVerifier { hostname, arg1 -> hostname.equals("clickcoffee.ru", ignoreCase = true) || hostname.equals("clients4.google.com", ignoreCase = true) || hostname.equals("api.admob.com", ignoreCase = true) }

            var context: SSLContext? = null
            if (trustManagers == null) {
                trustManagers = arrayOf(HttpsTrustManager())
            }

            try {
                context = SSLContext.getInstance("TLS")
                context!!.init(null, trustManagers, SecureRandom())
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            }

            HttpsURLConnection.setDefaultSSLSocketFactory(context!!
                    .socketFactory)
        }
    }

}
