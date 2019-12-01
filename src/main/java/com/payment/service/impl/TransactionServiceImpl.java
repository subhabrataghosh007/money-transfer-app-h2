package com.payment.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigDecimal;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;

import com.payment.constant.Message;
import com.payment.constant.TransactionCode;
import com.payment.dto.MoneyTransfer;
import com.payment.exception.NotFoundException;
import com.payment.exception.TransactionFailureException;
import com.payment.model.MoneyTransferModel;
import com.payment.repository.TransactionRepository;
import com.payment.repository.WalletRepository;
import com.payment.service.TransactionService;


@ApplicationScoped
public class TransactionServiceImpl implements TransactionService {

	private static final Logger LOGGER = getLogger(TransactionServiceImpl.class);
	
	@Inject
	TransactionRepository  transactionRepository;
	
	@Inject
	WalletRepository walletRepository;
	
	@Override
	public MoneyTransferModel transfer(MoneyTransfer trx) {
		return transferMoney(trx);
	}

	@Override
	public List<MoneyTransferModel> transactions() {
		return transactionRepository.findAllTransactions();
	}

	@Override
	public MoneyTransferModel getByTransactionId(String id) {
		return transactionRepository.getTransactionsByTransactionId(id);
	}

	@Override
	public List<MoneyTransferModel> getTransactionsBySender(String sender) {
		List<MoneyTransferModel> models = transactionRepository.getTransactionsBySender(sender);
		return models;
	}
	
	@Transactional
    public MoneyTransferModel transferMoney(MoneyTransfer transaction) {
        BigDecimal senderBalance = transactionRepository.getAccountBalance(transaction.getSender());
        if (transaction.getAmount().compareTo(senderBalance) > 0) {
            LOGGER.error("Insufficient balance to transfer money from {} to {}", transaction.getSender(),
                    transaction.getReceiver());

            throw new TransactionFailureException("Insufficient balance to transfer money. Wallet balance : " + senderBalance);
        }
        transactionRepository.debitMoney(transaction);
        
        if (!walletRepository.doesWalletExist(transaction.getReceiver())) {
            LOGGER.error("Wallet not registered for mobile number :: {}", transaction.getReceiver());
            throw new NotFoundException("Wallet not registered for mobile number - " + transaction.getReceiver());
        }
        transactionRepository.creditMoney(transaction);
        
        MoneyTransferModel txnModel = transactionRepository.addTransaction(transaction, Message.SUCCESS, TransactionCode.APPROVED);
        return txnModel;
    }
	
}
