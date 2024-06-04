package camp.letsbuild.inventoryscanner

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

typealias ServerErrorHandler = (call: Call<*>, response: Response<*>) -> Unit
typealias NetworkErrorHandler = (call: Call<*>, throwable: Throwable) -> Unit

abstract class AbstractChainedNetworkRequest<S, T>(private val parent: AbstractChainedNetworkRequest<*, S>?) {
    internal var child: AbstractChainedNetworkRequest<T, *>? = null

    private val defaultServerErrorHandler: ServerErrorHandler = { _, _ -> }
    private val defaultNetworkErrorHandler: NetworkErrorHandler = { _, _ -> }
    internal var serverErrorHandler = defaultServerErrorHandler
    internal var networkErrorHandler = defaultNetworkErrorHandler
    internal var retryNumber = 0

    abstract fun doExecute(s: S)

    open fun execute() {
        if (parent?.serverErrorHandler == defaultServerErrorHandler) {
            parent.serverErrorHandler = serverErrorHandler
        }
        if (parent?.networkErrorHandler == defaultNetworkErrorHandler) {
            parent.networkErrorHandler = networkErrorHandler
        }
        if (parent?.retryNumber == 0) {
            parent.retryNumber = retryNumber
        }
        parent?.execute()
    }
}
open class ChainedNetworkRequest<S, T>(parent: ChainedNetworkRequest<*, S>?, private val callProducer: (s: S) -> Call<T>) : AbstractChainedNetworkRequest<S, T>(parent) {
    companion object Factory {
        fun <Out> begin(call: Call<Out>): ChainedNetworkRequest<Unit, Out> {
            return object : ChainedNetworkRequest<Unit, Out>(null, {call}) {
                override fun execute() {
                    doExecute(Unit)
                }
            }
        }
    }

    //fun <U1, U2> thenInParallel(callProducer1: (t: T) -> Call<U1>, callProducer2: (t: T) -> Call<U2>)

    fun <U> then(callProducer: (t: T) -> Call<U>): ChainedNetworkRequest<T, U> {
        val retval =  ChainedNetworkRequest(this, callProducer)
        this.child = retval
        return retval
    }

    fun finally(function: (t: T) -> Unit): FinalChainedNetworkRequest<T> {
        val retval = FinalChainedNetworkRequest(this, function)
        this.child = retval
        return retval
    }

    override fun doExecute(s: S) {
        callProducer(s).enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful && response.body() != null) {
                    child?.doExecute(response.body()!!)
                } else {
                    serverErrorHandler(call, response)
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                if (retryNumber > 0) {
                    retryNumber--
                    call.enqueue(this)
                } else {
                    networkErrorHandler(call, t)
                }
            }
        })
    }
}

open class FinalChainedNetworkRequest<T>(parent: AbstractChainedNetworkRequest<*, T>?, private val function: (t: T) -> Unit) : AbstractChainedNetworkRequest<T, Unit>(parent) {
    override fun doExecute(s: T) {
        function(s)
    }

    fun handleServerErrorsWith(errorHandler: (call: Call<*>, response: Response<*>) -> Unit): FinalChainedNetworkRequest<T> {
        serverErrorHandler = errorHandler
        return this
    }

    fun handleNetworkErrorsWith(errorHandler: (call: Call<*>, throwable: Throwable) -> Unit): FinalChainedNetworkRequest<T> {
        networkErrorHandler = errorHandler
        return this
    }

    fun withNumberOfRetries(retries: Int): FinalChainedNetworkRequest<T> {
        retryNumber = retries
        return this
    }
}