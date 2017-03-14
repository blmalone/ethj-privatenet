package com.ethercamp.starter.rest;


import com.ethercamp.starter.ethereum.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class MyRestController {

    @Autowired
    Client client;

    private final int TIME_TO_WAIT = 30000;


    //GET endpoint for testing purposes.
    @RequestMapping(value = "test/contract", method = RequestMethod.GET, headers = "Accept=application/json")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public Map<String, String> contractIssue() throws IOException, InterruptedException {
            final Map<String, String> contractAddress = client.testContract();
            waitForTransactionToBeMined();
            client.testContractWrite(contractAddress.get("contractAddress"));
            waitForTransactionToBeMined();
            contractAddress.put("value: ",
                    String.valueOf(client.testContractRead(contractAddress.get("contractAddress"))));
        //Value is always Zero even though we write the value 515 to SimpleStorage contract.
            return contractAddress;
    }

    /**
     * Temporary fix so that we know when a transaction has been included into a block.
     * Helps us avoid invoking a function on the contract when it hasn't yet been published or
     * obtaining a nonce error.
     *
     * @throws InterruptedException
     */
    private void waitForTransactionToBeMined() throws InterruptedException {
        Thread.sleep(TIME_TO_WAIT);
    }


}
