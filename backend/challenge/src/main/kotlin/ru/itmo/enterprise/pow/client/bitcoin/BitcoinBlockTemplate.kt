package ru.itmo.enterprise.pow.client.bitcoin

data class BitcoinBlockTemplate(
    val version: Long,
    val previousBlockHash: String,
    val bits: String,
    val curTime: Long,
    val height: Long,
    val coinbaseValue: Long,
    val transactions: List<BitcoinTemplateTransaction>
)

data class BitcoinTemplateTransaction(
    val data: String
)
