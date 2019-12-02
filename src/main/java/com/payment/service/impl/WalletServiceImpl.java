package com.payment.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.payment.exception.DuplicateException;
import com.payment.exception.NotFoundException;
import com.payment.model.Wallet;
import com.payment.repository.WalletRepository;
import com.payment.service.WalletService;

@ApplicationScoped
public class WalletServiceImpl implements WalletService {

	private static final Logger LOGGER = getLogger(WalletServiceImpl.class);

	@Inject
	WalletRepository walletRepository;

	@Override
	public List<Wallet> allWallets() {
		return walletRepository.findAllWallets();
	}

	@Override
	public void addWallet(Wallet wallet) {

		if (walletRepository.doesWalletExist(wallet.getPhoneNumber())) {
			LOGGER.error("Wallet with mobile number {} already exists", wallet.getPhoneNumber());
			throw new DuplicateException("Wallet with mobile number " + wallet.getPhoneNumber() + " already exists");
		}

		walletRepository.createWallet(wallet);
	}

	@Override
	public void addMoney(Wallet wallet, String phoneNumber) {
		if (!walletRepository.doesWalletExist(phoneNumber)) {
			LOGGER.error("Wallet with mobile number {} not exists", phoneNumber);
			throw new NotFoundException("Wallet with mobile number " + phoneNumber + " not exists");
		}

		walletRepository.addMoneyToWallet(wallet, phoneNumber);
	}
}
