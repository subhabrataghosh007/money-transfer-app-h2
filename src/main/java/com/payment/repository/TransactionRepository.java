package com.payment.repository;

import static com.payment.constant.SQLConstant.CREDIT_MONEY_SQL;
import static com.payment.constant.SQLConstant.DEBIT_MONEY_SQL;
import static com.payment.constant.SQLConstant.FETCH_BALANCE_SQL;
import static com.payment.constant.SQLConstant.CREATE_TRANSACTION_SQL;
import static com.payment.constant.SQLConstant.FIND_ALL_TRANSACTION;
import static com.payment.constant.SQLConstant.FIND_ALL_TRANSACTION_BY_SENDER;
import static com.payment.constant.SQLConstant.FIND_ALL_TRANSACTION_BY_TRANSACTIONID;
import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.payment.constant.Message;
import com.payment.constant.TransactionCode;
import com.payment.dto.MoneyTransfer;
import com.payment.exception.InternalException;
import com.payment.exception.NotFoundException;
import com.payment.exception.TransactionFailureException;
import com.payment.model.MoneyTransferModel;

import io.agroal.api.AgroalDataSource;

@Singleton
public class TransactionRepository {
	private static final Logger LOGGER = getLogger(TransactionRepository.class);

	@Inject
	AgroalDataSource dataSource;

	public BigDecimal getAccountBalance(String mobileNumber) {
		
		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(FETCH_BALANCE_SQL)) {

			LOGGER.info("Fetching Wallet data from Wallet table for mobile number :: {}", mobileNumber);
			preparedStatement.setString(1, mobileNumber);
			
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				
				if (resultSet.next()) {
					BigDecimal balance = resultSet.getBigDecimal(1);
					LOGGER.info("Wallet balance successfully fetched for mobile number :: {}", mobileNumber);
					return balance;
				} else {
					LOGGER.error("Wallet not registered for mobile number :: {}", mobileNumber);
					throw new NotFoundException("Wallet not registered for mobile number :: " + mobileNumber);

				}
			}
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to fetch data from Wallet table", ex);
			throw new InternalException("Error occurred to fetch data from Wallet table");
		}
	}

	public void debitMoney(MoneyTransfer transaction) {
		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(DEBIT_MONEY_SQL)) {

			LOGGER.info("Debiting money from wallet (Mobile number - {})", transaction.getSender());
			preparedStatement.setBigDecimal(1, transaction.getAmount());
			preparedStatement.setString(2, transaction.getSender());
			int updateCount = preparedStatement.executeUpdate();
			
			if (updateCount == 1) {
				LOGGER.info("Wallet balance updated for mobile number :: {}", transaction.getSender());
			} else {
				LOGGER.error("Failed to update wallet balance for mobile number :: {}", transaction.getSender());
				throw new TransactionFailureException(
						"Failed to update wallet balance for mobile number " + transaction.getSender());
			}
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to update balance in wallet table", ex);
			throw new TransactionFailureException("Error occurred to update balance in wallet table");
		}
	}

	public void creditMoney(MoneyTransfer transaction) {
		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(CREDIT_MONEY_SQL)) {

			LOGGER.info("Crediting money to wallet (Mobile number - {})", transaction.getReceiver());
			preparedStatement.setBigDecimal(1, transaction.getAmount());
			preparedStatement.setString(2, transaction.getReceiver());
			int updateCount = preparedStatement.executeUpdate();
			
			if (updateCount == 1) {
				LOGGER.info("Wallet balance updated for mobile number :: {}", transaction.getReceiver());
			} else {
				LOGGER.error("Failed to update wallet balance for mobile number :: {}", transaction.getReceiver());
				throw new TransactionFailureException(
						"Failed to update wallet balance for mobile number " + transaction.getReceiver());
			}
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to update balance in Wallet table", ex);
			throw new TransactionFailureException("Error occurred to update balance - " + ex.getMessage());
		}
	}

	public MoneyTransferModel addTransaction(MoneyTransfer transaction, Message message, TransactionCode code) {

		MoneyTransferModel txnModel = createTransationModel(transaction, message, code);
		
		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(CREATE_TRANSACTION_SQL)) {

			LOGGER.info("Inserting Transaction data into Transactions table");
			preparedStatement.setString(1, txnModel.getTransactionId().toString());
			preparedStatement.setString(2, txnModel.getSender());
			preparedStatement.setString(3, txnModel.getReceiver());
			preparedStatement.setBigDecimal(4, txnModel.getAmount());
			preparedStatement.setString(5, txnModel.getTag());
			preparedStatement.setString(6, txnModel.getStatus());
			preparedStatement.setString(7, txnModel.getTransferDateTime());
			preparedStatement.setString(8, txnModel.getStatusCode());
			preparedStatement.setString(9, txnModel.getDescription());
			preparedStatement.execute();
			LOGGER.info("Wallet data successfully inserted into Account table");
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to insert data into Wallet table", ex);
			throw new InternalException("Error occurred to fetch data from Wallet table");
		}
		return txnModel;
	}
	
	private MoneyTransferModel createTransationModel(MoneyTransfer trx, Message message, TransactionCode code) {
		MoneyTransferModel txnModel = new MoneyTransferModel.MoneyTransferModelBuilder()
				.setSender(trx.getSender())
				.setReceiver(trx.getReceiver())
				.setAmount(trx.getAmount())
				.setTag(trx.getTag())
				.setStatus(message.getMessage())
				.setTransferDateTime(LocalDateTime.now().toString())
				.setDescription(code.getMessage())
				.setStatusCode(code.getCode())
				.build();
		return txnModel;
	}
	
	public List<MoneyTransferModel> findAllTransactions() {

		List<MoneyTransferModel> transferModels = new LinkedList<>();
		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(FIND_ALL_TRANSACTION)) {

			LOGGER.info("Retrieving all Transactions");

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				transferModels = transactions(resultSet);
			}
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to fetch data from Transactions table", ex);
			throw new InternalException("Error occurred to fetch data from Transactions table");
		}
		return transferModels;
	}
	
	public List<MoneyTransferModel> getTransactionsBySender(String sender) {
		
		List<MoneyTransferModel> transferModels = new LinkedList<>();
		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(FIND_ALL_TRANSACTION_BY_SENDER)) {
			preparedStatement.setString(1, sender);

			LOGGER.info("Retrieving all Transactions by "+ sender);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				transferModels = transactions(resultSet);
			}
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to fetch data from Transactions table", ex);
			throw new InternalException("Error occurred to fetch data from Transactions table");
		}
		return transferModels;
	}
	

	public MoneyTransferModel getTransactionsByTransactionId(String id) {
		MoneyTransferModel transferModel = null;
		
		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(FIND_ALL_TRANSACTION_BY_TRANSACTIONID)) {
			preparedStatement.setString(1, id);

			LOGGER.info("Retrieving all Transactions by "+ id);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					transferModel = getMoneyTransferModelFromRestlSet(resultSet);
				}
			}
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to fetch data from Transactions table", ex);
			throw new InternalException("Error occurred to fetch data from Transactions table");
		}
		return transferModel;
	}

	private List<MoneyTransferModel> transactions(ResultSet resultSet) {
		List<MoneyTransferModel> transferModels = new LinkedList<>();

		try {
			while (resultSet.next()) {
				
				MoneyTransferModel model = getMoneyTransferModelFromRestlSet(resultSet);
				transferModels.add(model);
			}
		} catch (SQLException e) {
			LOGGER.error("Error occurred to fetch data from Transactions table", e);
			throw new InternalException("Error occurred to fetch data from Transactions table");
		}

		return transferModels;
	}
	
	private MoneyTransferModel getMoneyTransferModelFromRestlSet(ResultSet resultSet) {
		MoneyTransferModel model = null;

		try {
			model = new MoneyTransferModel.MoneyTransferModelBuilder()
					.setTransactionId(resultSet.getString("TransactionId"))
					.setAmount(new BigDecimal(resultSet.getString("Amount")))
					.setDescription(resultSet.getString("Description"))
					.setReceiver(resultSet.getString("Receiver"))
					.setSender(resultSet.getString("Sender"))
					.setStatus(resultSet.getString("Status"))
					.setStatusCode(resultSet.getString("StatusCode"))
					.setTag(resultSet.getString("Tag"))
					.setTransferDateTime(resultSet.getString("TransferDateTime")).build();

		} catch (SQLException e) {
			throw new InternalException("Error occurred to fetch data from Transactions table");
		}

		return model;
	}

}
