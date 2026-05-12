package com.gonzalez.trophychest.data

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class WorkersDevFallbackDnsTest {
    @Test
    fun workerHostPrefersReachableWorkersDevEdges() {
        val workerAddress = ipv4(188, 114, 96, 5)
        val fallbackAddress = ipv4(104, 18, 13, 15)
        val dns = WorkersDevFallbackDns(
            systemDns = FakeDns(
                mapOf(
                    "trophychest-igdb-proxy.trophychest.workers.dev" to listOf(workerAddress),
                    "workers.dev" to listOf(fallbackAddress)
                )
            )
        )

        val addresses = dns.lookup("trophychest-igdb-proxy.trophychest.workers.dev")

        assertEquals(listOf(fallbackAddress, workerAddress), addresses)
    }

    @Test
    fun nonWorkerHostUsesSystemDnsOnly() {
        val steamAddress = ipv4(23, 45, 67, 89)
        val dns = WorkersDevFallbackDns(
            systemDns = FakeDns(
                mapOf(
                    "store.steampowered.com" to listOf(steamAddress),
                    "workers.dev" to listOf(ipv4(104, 18, 13, 15))
                )
            )
        )

        val addresses = dns.lookup("store.steampowered.com")

        assertEquals(listOf(steamAddress), addresses)
    }

    @Test
    fun duplicateAddressesAreRemoved() {
        val sharedAddress = ipv4(104, 18, 13, 15)
        val workerAddress = ipv4(188, 114, 96, 5)
        val dns = WorkersDevFallbackDns(
            systemDns = FakeDns(
                mapOf(
                    "trophychest-igdb-proxy.trophychest.workers.dev" to listOf(sharedAddress, workerAddress),
                    "workers.dev" to listOf(sharedAddress)
                )
            )
        )

        val addresses = dns.lookup("trophychest-igdb-proxy.trophychest.workers.dev")

        assertEquals(listOf(sharedAddress, workerAddress), addresses)
    }

    private class FakeDns(
        private val addressesByHost: Map<String, List<InetAddress>>
    ) : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return addressesByHost.getValue(hostname)
        }
    }

    private companion object {
        fun ipv4(a: Int, b: Int, c: Int, d: Int): InetAddress {
            return InetAddress.getByAddress(
                byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte())
            )
        }
    }
}
