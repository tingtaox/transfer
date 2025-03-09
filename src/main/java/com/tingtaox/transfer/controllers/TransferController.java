package com.tingtaox.transfer.controllers;

import com.tingtaox.transfer.models.TransferRequest;
import com.tingtaox.transfer.services.TransferService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class TransferController {

	private static final Logger logger = LogManager.getLogger(TransferController.class);

	@Autowired
	private TransferService transferService;

	@GetMapping("/balance/{account}")
	public BigDecimal getBalance(@PathVariable("account") String account) {
		logger.info("Get balance from account {}", account);
		return transferService.getBalance(account);
	}

	@PostMapping("/transfer")
	public String transfer(@RequestBody TransferRequest request) {
		logger.info("Transfer money from {} to {} with amount {}",
				request.getFromAccount(), request.getToAccount(), request.getAmount());
		return transferService.transfer(request);
	}
}
