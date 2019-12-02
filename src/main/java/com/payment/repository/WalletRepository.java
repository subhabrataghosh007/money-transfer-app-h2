package com.payment.repository;

import io.agroal.api.AgroalDataSource;
import org.slf4j.Logger;

import com.payment.exception.InternalException;
import com.payment.model.Wallet;


import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import static com.payment.constant.SQLConstant.WALLET_CHECK_SQL;
import static com.payment.constant.SQLConstant.CREATE_WALLET_SQL;
import static com.payment.constant.SQLConstant.FIND_ALL_WALLET;
import static com.payment.constant.SQLConstant.CREDIT_MONEY_SQL;
import static java.lang.Long.parseLong;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class WalletRepository {
	
	private static final Logger LOGGER = getLogger(WalletRepository.class);

	@Inject
	AgroalDataSource dataSource;

	public boolean doesWalletExist(String mobileNumber) {
		
		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(WALLET_CHECK_SQL)) {

			boolean isAccountPresent = false;
			LOGGER.info("Checking if Wallet with mobile number {} exists", mobileNumber);
			preparedStatement.setLong(1, parseLong(mobileNumber));
			
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next() && resultSet.getInt(1) != 0)
					isAccountPresent = true;
			}
			return isAccountPresent;
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to fetch data from Wallets table", ex);
			throw new InternalException("Error occurred to fetch data from Wallets table");
		}
	}
	
	public List<Wallet> findAllWallets() {

		List<Wallet> wallets = new LinkedList<>();

		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(FIND_ALL_WALLET)) {

			LOGGER.info("Retrieving all wallets");

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				
				while (resultSet.next()) {
					
					Wallet wallet = new Wallet();
					wallet.setPhoneNumber(resultSet.getObject("PHONE_NUMBER").toString());
					wallet.setBalance(resultSet.getBigDecimal("BALANCE"));

					wallets.add(wallet);
				}
			}
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to fetch data from Wallets table", ex);
			throw new InternalException("Error occurred to fetch data from Wallets table");
		}

		return wallets;
	}

	public void createWallet(Wallet wallet) {
		
		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(CREATE_WALLET_SQL)) {

			LOGGER.info("Inserting Wallets data into Accounts table");
			preparedStatement.setString(1, wallet.getPhoneNumber());
			preparedStatement.setBigDecimal(2, wallet.getBalance());
			preparedStatement.execute();
			LOGGER.info("Wallet data successfully inserted into Wallets table");
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to insert data into Wallets table", ex);
			throw new InternalException("Error occurred to fetch data from Accounts table");
		}
	}

	public void addMoneyToWallet(Wallet wallet, String phoneNumber) {

		try (Connection conn = dataSource.getConnection();
				PreparedStatement preparedStatement = conn.prepareStatement(CREDIT_MONEY_SQL)) {

			LOGGER.info("Debiting money from account (Mobile number - {})", phoneNumber);
			preparedStatement.setBigDecimal(1, wallet.getBalance());
			preparedStatement.setString(2, phoneNumber);
			int updateCount = preparedStatement.executeUpdate();
			
			if (updateCount == 1) {
				LOGGER.info("Wallets balance updated for mobile number :: {}", phoneNumber);
			} else {
				LOGGER.error("Failed to update Wallets balance for mobile number :: {}", phoneNumber);
				throw new InternalException(
						"Failed to update Wallet balance for mobile number " + phoneNumber);
			}
		} catch (SQLException ex) {
			LOGGER.error("Error occurred to update balance in Wallets table", ex);
			throw new InternalException("Error occurred to update balance in Wallets table" + ex.getMessage());
		}

	}
}
