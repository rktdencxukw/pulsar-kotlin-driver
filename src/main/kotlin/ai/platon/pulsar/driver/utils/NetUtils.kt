package ai.platon.pulsar.driver.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.regex.Pattern


object NetUtils {
    /**
     * 外网ip地址
     */
    private var publicIp: String? = null

    /**
     * 下面url返回地址都包含ip地址，为防止某个url失效，
     * 遍历url获取ip地址，有一个能成功获取就返回
     */
    private val urls = arrayOf(
        "http://whatismyip.akamai.com",
        "http://icanhazip.com",
        "http://members.3322.org/dyndns/getip",
        "http://checkip.dyndns.com/",
        "http://pv.sohu.com/cityjson",
        "http://ip.taobao.com/service/getIpInfo.php?ip=myip",
        "http://www.ip168.com/json.do?view=myipaddress",
        "http://www.net.cn/static/customercare/yourip.asp",
        "http://ipecho.net/plain",
        "http://myip.dnsomatic.com",
        "http://tnx.nl/ip",
        "http://ifconfig.me"
    )

    /**
     * ip地址的匹配正则表达式
     */
    private const val regEx = "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)"
    private val pattern = Pattern.compile(regEx)
    val selfPublicIp: String?
        /**
         * 获取本机外网地址
         *
         * @return
         */
        get() {
            if (publicIp != null && "" != publicIp!!.trim { it <= ' ' }) {
                return publicIp
            }
            for (url in urls) {
                //http访问url获取带ip的信息
                val result = getUrlResult(url)
                //正则匹配查找ip地址
                val m = pattern.matcher(result)
                while (m.find()) {
                    publicIp = m.group()
                    //                System.out.println(url + " ==> " + publicIp);
                    //只获取匹配到的第一个IP地址
                    return publicIp
                }
            }
            return null
        }

    /**
     * http访问url
     */
    private fun getUrlResult(url: String): String {
        val sb = StringBuilder()
        var `in`: BufferedReader? = null
        try {
            val realUrl = URL(url)
            val connection = realUrl.openConnection()
            connection.connectTimeout = 1000
            connection.readTimeout = 1000
            `in` = BufferedReader(InputStreamReader(connection.getInputStream()))
            var line: String?
            while (`in`.readLine().also { line = it } != null) {
                sb.append(line)
            }
        } catch (e: Exception) {
//            System.out.println(e.getMessage());
            return ""
        }
        return sb.toString()
    }

    fun getInternalIps(): List<String> {
        val ips = mutableListOf<String>()
        val nifs = NetworkInterface.getNetworkInterfaces()
        while (nifs.hasMoreElements()) {
            val nif = nifs.nextElement()
            val address = nif.inetAddresses
            while (address.hasMoreElements()) {
                val addr = address.nextElement()
                if (addr is Inet4Address) {
                    ips.add(addr.getHostAddress())
//                    println("网卡名称：" + nif.name)
//                    println("网络接口地址：" + addr.getHostAddress())
                }
            }
        }
        return ips
    }

    /**
     * 测试
     * @param args
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println(selfPublicIp)
        println(getInternalIps())
    }
}
