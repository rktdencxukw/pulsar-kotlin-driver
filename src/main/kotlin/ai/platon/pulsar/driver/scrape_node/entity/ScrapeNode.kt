package ai.platon.pulsar.driver.scrape_node.entity

import ai.platon.pulsar.persist.metadata.FetchMode
import ai.platon.pulsar.persist.metadata.IpType

class ScrapeNode {
    var nodeId: String? = null
    lateinit var ipType: IpType
    lateinit var fetchModeSupport: List<FetchMode>
    var online: Boolean = false
    lateinit var userName: String
}