package de.jepfa.yapm.util

import de.jepfa.yapm.util.IpConverter
import de.jepfa.yapm.util.IpConverter.PrivateNetworkClassification.*
import org.junit.Assert
import org.junit.Test

class IpConverterTest {

    @Test
    fun testClassifyA() {
        Assert.assertEquals(OneClassA, IpConverter.classifyIp("10.0.0.0"))
        Assert.assertEquals(OneClassA, IpConverter.classifyIp("10.10.10.10"))
        Assert.assertEquals(OneClassA, IpConverter.classifyIp("10.255.255.255"))
        Assert.assertNull(IpConverter.classifyIp("9.255.255.255"))
        Assert.assertNull(IpConverter.classifyIp("11.0.0.0"))
    }

    @Test
    fun testClassifyB() {
        Assert.assertEquals(ManyClassB, IpConverter.classifyIp("172.16.0.0"))
        Assert.assertEquals(ManyClassB, IpConverter.classifyIp("172.20.0.12"))
        Assert.assertEquals(ManyClassB, IpConverter.classifyIp("172.31.255.255"))
        Assert.assertNull(IpConverter.classifyIp("172.15.255.255"))
        Assert.assertNull(IpConverter.classifyIp("172.32.0.0"))
    }

    @Test
    fun testClassifyC() {
        Assert.assertEquals(ManyClassC, IpConverter.classifyIp("192.168.0.0"))
        Assert.assertEquals(ManyClassC, IpConverter.classifyIp("192.168.10.130"))
        Assert.assertEquals(ManyClassC, IpConverter.classifyIp("192.168.255.255"))
        Assert.assertNull(IpConverter.classifyIp("192.167.255.255"))
        Assert.assertNull(IpConverter.classifyIp("192.169.0.0"))
    }

    @Test
    fun testIpToLong() {
        Assert.assertEquals(20L, IpConverter.ipToLong("0.0.0.20"))
        Assert.assertEquals(512L, IpConverter.ipToLong("0.0.2.0"))
        Assert.assertEquals(65536L, IpConverter.ipToLong("0.1.0.0"))
        Assert.assertEquals(16777216L, IpConverter.ipToLong("01.0.0.0"))
        Assert.assertEquals(4294967295L, IpConverter.ipToLong("255.255.255.255"))
    }

    @Test
    fun testToBasedString() {
        Assert.assertEquals("u", IpConverter.toBasedString(20L))
    }
    @Test
    fun testGetHandle() {
        Assert.assertEquals("@ib", IpConverter.getHandle("192.168.0.1")) //ClassC min shortest handle
        Assert.assertEquals("@ib", IpConverter.getHandle("192.168.0.1")) //ClassC
        Assert.assertEquals("@ikj", IpConverter.getHandle("192.168.1.13")) //ClassC
        Assert.assertEquals("@icpmd", IpConverter.getHandle("192.168.178.39")) //ClassC

        Assert.assertEquals("@eb", IpConverter.getHandle("172.16.0.1")) //ClassB
        Assert.assertEquals("@ecpmd", IpConverter.getHandle("172.16.178.39")) //ClassB

        Assert.assertEquals("@ab", IpConverter.getHandle("10.0.0.1")) //ClassA
        Assert.assertEquals("@abksojn", IpConverter.getHandle("10.255.255.255")) //ClassA max longest handle

        Assert.assertEquals("?", IpConverter.getHandle("255.255.255.255")) // Invalid internal IP
        Assert.assertEquals("?", IpConverter.getHandle("grab")) // Invalid internal IP
    }

}