package org.walleth.ui

import android.text.format.DateUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.transaction_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kethereum.functions.getTokenRelevantTo
import org.kethereum.functions.getTokenRelevantValue
import org.ligi.kaxt.setVisibility
import org.walleth.R
import org.walleth.activities.getTransactionActivityIntentForHash
import org.walleth.data.AppDatabase
import org.walleth.data.addressbook.resolveNameAsync
import org.walleth.data.config.Settings
import org.walleth.data.exchangerate.ExchangeRateProvider
import org.walleth.data.networks.ChainInfoProvider
import org.walleth.data.tokens.getRootToken
import org.walleth.data.transactions.TransactionEntity
import org.walleth.ui.valueview.ValueViewController

class TransactionViewHolder(itemView: View,
                            private val direction: TransactionAdapterDirection,
                            val chainInfoProvider: ChainInfoProvider,
                            private val exchangeRateProvider: ExchangeRateProvider,
                            val settings: Settings) : RecyclerView.ViewHolder(itemView) {


    private val amountViewModel by lazy {
        ValueViewController(itemView.difference, exchangeRateProvider, settings)
    }

    fun bind(transactionWithState: TransactionEntity?, appDatabase: AppDatabase) {

        if (transactionWithState != null) {
            val transaction = transactionWithState.transaction

            val relevantAddress = if (direction == TransactionAdapterDirection.INCOMING) {
                transaction.from
            } else {
                transaction.getTokenRelevantTo() ?: transaction.to
            }

            val tokenValue = transaction.getTokenRelevantValue()
            if (tokenValue != null) {
                val tokenAddress = transaction.to
                if (tokenAddress != null) {
                    GlobalScope.launch {
                        appDatabase.tokens.forAddress(tokenAddress)?.let {
                            amountViewModel.setValue(tokenValue, it)
                        }
                    }
                }
            } else {
                amountViewModel.setValue(transaction.value, chainInfoProvider.getCurrent()?.getRootToken())
            }

            relevantAddress?.let {
                appDatabase.addressBook.resolveNameAsync(it) { name ->
                    itemView.address.text = name
                }
            }

            itemView.transaction_err.setVisibility(transactionWithState.transactionState.error != null)
            if (transactionWithState.transactionState.error != null) {
                itemView.transaction_err.text = transactionWithState.transactionState.error
            }

            val epochMillis = (transaction.creationEpochSecond ?: 0) * 1000L
            val context = itemView.context
            itemView.date.text = DateUtils.getRelativeDateTimeString(context, epochMillis,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    0
            )

            itemView.transaction_state_indicator.setImageResource(
                    when {
                        !transactionWithState.transactionState.isPending -> R.drawable.ic_lock_black_24dp
                        transactionWithState.signatureData == null -> R.drawable.ic_lock_open_black_24dp
                        else -> R.drawable.ic_lock_outline_24dp
                    }
            )

            itemView.isClickable = true
            itemView.setOnClickListener {
                transactionWithState.hash.let {
                    context.startActivity(context.getTransactionActivityIntentForHash(it))
                }

            }
        }
    }

}