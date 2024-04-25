package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
	@Autowired
	private CreditCardRepository creditCardRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        Optional<User> user = userRepository.findById(payload.getUserId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        CreditCard creditCard = new CreditCard();
        creditCard.setNumber(payload.getCardNumber());
        creditCard.setIssuanceBank(payload.getCardIssuanceBank());
        creditCard.setOwner(user.get());
        creditCardRepository.save(creditCard);
        return ResponseEntity.ok(creditCard.getId());
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<CreditCard> creditCards = creditCardRepository.findByOwner(user.get());
        List<CreditCardView> creditCardViews = creditCards.stream()
                .map(card -> new CreditCardView(card.getIssuanceBank(), card.getNumber()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(creditCardViews);

        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null

    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        Optional<CreditCard> creditCard = creditCardRepository.findByNumber(creditCardNumber);
        return creditCard.map(card -> ResponseEntity.ok(card.getOwner().getId())).orElseGet(() -> ResponseEntity.badRequest().build());
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> updateBalance(@RequestBody UpdateBalancePayload[] payload) {
        for (UpdateBalancePayload updateBalancePayload : payload) {
            Optional<CreditCard> creditCardOpt = creditCardRepository.findByNumber(updateBalancePayload.getCreditCardNumber());
            if (creditCardOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Credit card not found");
            }
            CreditCard creditCard = creditCardOpt.get();
            BalanceHistory balanceHistory = creditCard.getBalanceHistory().stream()
                    .filter(bh -> bh.getDate().equals(updateBalancePayload.getBalanceDate()))
                    .findFirst()
                    .orElseGet(() -> {
                        BalanceHistory newBalance = new BalanceHistory();
                        newBalance.setDate(updateBalancePayload.getBalanceDate());
                        newBalance.setBalance(updateBalancePayload.getBalanceAmount());
                        newBalance.setCreditCard(creditCard);
                        creditCard.getBalanceHistory().add(newBalance);
                        return newBalance;
                    });
            double difference = updateBalancePayload.getBalanceAmount() - balanceHistory.getBalance();
            if (difference != 0) {
                balanceHistory.setBalance(updateBalancePayload.getBalanceAmount());
                creditCard.getBalanceHistory().stream()
                        .filter(bh -> bh.getDate().isAfter(updateBalancePayload.getBalanceDate()))
                        .forEach(bh -> bh.setBalance(bh.getBalance() + difference));
            }
            creditCardRepository.save(creditCard);
        }
        return ResponseEntity.ok("Balance updated successfully");

        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.

    }

}
