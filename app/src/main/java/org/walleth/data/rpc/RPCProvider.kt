package org.walleth.data.rpc

import android.content.Context
import okhttp3.OkHttpClient
import org.kethereum.model.ChainId
import org.kethereum.rpc.BaseEthereumRPC
import org.kethereum.rpc.ConsoleLoggingTransportWrapper
import org.kethereum.rpc.EthereumRPC
import org.kethereum.rpc.HttpTransport
import org.kethereum.rpc.min3.getMin3RPC
import org.walleth.chains.ChainInfoProvider
import org.walleth.data.AppDatabase
import org.walleth.data.chaininfo.ChainInfo
import org.walleth.util.getRPCEndpoint
import java.math.BigInteger
import java.math.BigInteger.ONE

const val KEY_IN3_RPC = "in3"

interface RPCProvider {
    suspend fun get(): EthereumRPC?
    suspend fun getForChain(chainId: ChainId): EthereumRPC?
}

class RPCProviderImpl(val context: Context,
                      var network: ChainInfoProvider,
                      var appDatabase: AppDatabase,
                      var okHttpClient: OkHttpClient) : RPCProvider {

    private fun ChainInfo.get(): BaseEthereumRPC? {

        return if (rpc.contains(KEY_IN3_RPC) && (chainId == ONE || chainId == BigInteger.valueOf(5))) {
            getMin3RPC(ChainId(chainId))
        } else getRPCEndpoint()?.let {
            BaseEthereumRPC(ConsoleLoggingTransportWrapper(HttpTransport(it, okHttpClient)))
        }
    }

    override suspend fun getForChain(chainId: ChainId) = appDatabase.chainInfo.getByChainId(chainId.value)?.get()

    override suspend fun get(): EthereumRPC? = network.getCurrent()?.get()

}