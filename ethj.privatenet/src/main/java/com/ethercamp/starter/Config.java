package com.ethercamp.starter;

import com.ethercamp.starter.ethereum.Client;
import com.ethercamp.starter.ethereum.ClientMiner;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class Config {

    @Bean
    Client client() throws Exception {
        ThreadFactory namedThreadFactory = getThreadFactory("Client");
        Client client = new Client();
        Executors.newSingleThreadExecutor(namedThreadFactory).
                submit(client::start);
        return client;
    }

    /**
     * Creating an Ethereum node solely tasked with mining transactions into blocks.
     */
    @Bean
    ClientMiner clientMiner() throws Exception {
        ThreadFactory namedThreadFactory = getThreadFactory("ClientMiner");
        ClientMiner clientMiner = new ClientMiner();
        Executors.newSingleThreadExecutor(namedThreadFactory).
                submit(clientMiner::start);
        return clientMiner;
    }

    private ThreadFactory getThreadFactory(final String threadName) {
        return new ThreadFactoryBuilder()
                .setNameFormat(threadName).build();
    }
}
