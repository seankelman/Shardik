package com.star_dot_star.shardik.service

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.star_dot_star.shardik.MainActivity
import org.pcap4j.packet.DnsPacket
import org.pcap4j.packet.IpV4Packet
import org.pcap4j.packet.UdpPacket
import org.pcap4j.packet.UnknownPacket
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class AppVpnService : VpnService() {
    private var thread: Thread? = null
    private val dnsServer = InetAddress.getByName("8.8.8.8")
    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var blacklistedHosts: HostBlacklist

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "service onStartCommand")

        blacklistedHosts = HostBlacklist(this)

        val notificationBuilder = NotificationCompat.Builder(this, MainActivity.NOTIFICATION_CHANNEL)
        startForeground(1234, notificationBuilder.build())

        vpnInterface = Builder()
            .addAddress("192.168.50.1", 24)
            .addDnsServer("192.168.50.5")
            .addRoute("0.0.0.0", 24)
            .setBlocking(true)
            .setSession("ShardikVPNService")
            .establish()

        thread?.interrupt()
        thread = Thread(Runnable {
            try {
                Log.i(TAG, "thread start")

                // VpnService gives us an input stream to read incoming bytes:
                val inputStream = FileInputStream(vpnInterface?.fileDescriptor)

                val packet = ByteArray(32767)

                // loop until this thread is killed:
                while (true) {
                    // Read one packet at a time:
                    inputStream.read(packet)

                    // Set up output stream
                    val outputStream = FileOutputStream(vpnInterface?.fileDescriptor)

                    // Protect "allowed" packets from VPN connections
                    val dnsSocket = DatagramSocket()
                    protect(dnsSocket)

                    // Handle each packet: either block it for ads or pass through for everything else
                    handlePacket(packet, dnsSocket, outputStream)
                }
            } catch (e: Exception) {
                // TODO: better error handling
                e.printStackTrace()
            } finally {
                Log.i(TAG, "thread cleanup")
                vpnInterface?.close()
                vpnInterface = null
            }
        })

        thread?.start()

        return Service.START_STICKY
    }

    private fun handlePacket(packetBytes: ByteArray, dnsSocket: DatagramSocket, outputStream: FileOutputStream) {
        try {
            val packet = IpV4Packet.newPacket(packetBytes, 0, packetBytes.size)

            // We only care about DNS packets:
            val udpPacket = packet.payload as UdpPacket? ?: return
            val dnsPacket = udpPacket.payload as DnsPacket? ?: return

            // Read the host name out of DNS packet:
            val dnsHostName = if (dnsPacket.header.questions.isNotEmpty()) {
                dnsPacket.header.questions[0].qName.name
            } else {
                ""
            }

            // Check hostname vs list of blacklisted hosts:
            val response: ByteArray = if (blacklistedHosts.isHostBlackListed(dnsHostName)) {
                Log.i(TAG, "found host $dnsHostName :: blocking")
                dnsPacket.rawData
            } else {
                Log.i(TAG, "found host $dnsHostName :: passthrough")
                val rawPacketData = udpPacket.payload.rawData
                val outPacket = DatagramPacket(rawPacketData, 0, rawPacketData.size, dnsServer, 53)

                dnsSocket.send(outPacket)

                val datagramData = ByteArray(1024)
                val replyPacket = DatagramPacket(datagramData, datagramData.size)
                dnsSocket.receive(replyPacket)
                datagramData
            }

            // Construct the outgoing packet
            val ipOutPacket = IpV4Packet.Builder(packet)
                .srcAddr(packet.header.dstAddr)
                .dstAddr(packet.header.srcAddr)
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .payloadBuilder(
                    UdpPacket.Builder(udpPacket)
                        .srcPort(udpPacket.header.dstPort)
                        .dstPort(udpPacket.header.srcPort)
                        .srcAddr(packet.header.dstAddr)
                        .dstAddr(packet.header.srcAddr)
                        .correctChecksumAtBuild(true)
                        .correctLengthAtBuild(true)
                        .payloadBuilder(
                            UnknownPacket.Builder()
                                .rawData(response)
                        )
                ).build()

            // Forward the outgoing packet by writing it to the output stream
            outputStream.write(ipOutPacket.rawData)
        } catch (e: Exception) {
            // TODO: error handling
            Log.e(TAG, e.message)
        } finally {
            dnsSocket.close()
            outputStream.close()
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "service onDestroy")
        thread?.interrupt()
        thread = null
        stopSelf()
    }

    companion object {
        private const val TAG = "AppVpnService"
    }
}