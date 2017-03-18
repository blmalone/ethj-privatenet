package com.ethercamp.starter.ethereum;


import com.typesafe.config.ConfigFactory;
import com.ethercamp.starter.service.FileReaderService;
import com.ethercamp.starter.service.FileReaderServiceImpl;
import com.ethercamp.starter.service.SolidityCompilerService;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.crypto.ECKey;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.program.ProgramResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.synchronizedMap;
import static org.bouncycastle.util.encoders.Hex.decode;
import static org.bouncycastle.util.encoders.Hex.toHexString;

/**
 * Created by blainemalone on 11/12/2016.
 */
@Service
public class Client extends EthjNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
    private static final int GAS_LIMIT = 3_000_000;
    private static final long TRANSACTION_VALUE = 2100000000000000000L;
    private static final int TRANSACTION_THREAD_SLEEP = 16000;
    private static final int ETHER_TO_TRANSFER_IN_WEI = 0;

    @Value("${ellipticcurve.key.private}")
    private String privateKey;

    private static final String CLIENT_CONFIG = "client.conf";
    private final FileReaderService fileReaderService = new FileReaderServiceImpl();
    private final SolidityCompilerService solidityCompilerService = new SolidityCompilerService();
    private Map<ByteArrayWrapper, Transaction> pendingTransactions = synchronizedMap(new HashMap<>());

    @Override
    public void start() {
        setEthereum(EthereumFactory.createEthereum(Client.class));
        getEthereum().addListener(new ClientListenerAdapter(this));
        establishConnection();
    }

    /**
     * Helps us override the default system properties for the ethereum instance.
     */
    @Bean
    public SystemProperties systemProperties() throws IOException {
        SystemProperties props = new SystemProperties();
        props.overrideParams(ConfigFactory.parseString(
                fileReaderService.readConfigFile(CLIENT_CONFIG).replaceAll("'", "\"")));
        setSystemProperties(props);
        return props;
    }

    public Map<String, String> sendFundsToWallet(final String walletAddress) throws InterruptedException {
        LOGGER.info("Sending funds to users wallet {}", walletAddress);
        final ECKey senderKey = ECKey.fromPrivate(decode(privateKey));

        Transaction fundsTransferTransaction =
                sendTransaction(walletAddress, senderKey, TRANSACTION_VALUE, new byte[0]);
        if (fundsTransferTransaction != null) {
            Map<String, String> transactionsData = new HashMap<>();
            transactionsData.put("fundsTransactionHash", toHexString(fundsTransferTransaction.getHash()));
            return transactionsData;
        } else {
            return null;
        }
    }
    public Map<String, String> testContract()
            throws IOException, InterruptedException {
        final ECKey senderKey = ECKey.fromPrivate(decode(privateKey));
        //Here I am compiling the contract in order to obtain the binary.
        CompilationResult.ContractMetadata contractBinary = solidityCompilerService.compileContract();
        Transaction contractTransaction =
                sendTransaction(null, senderKey, ETHER_TO_TRANSFER_IN_WEI, decode(contractBinary.bin));
        if (contractTransaction != null) {
            Map<String, String> transactionsData = new HashMap<>();
            transactionsData.put("contractAddress", toHexString(contractTransaction.getContractAddress()));
            return transactionsData;
        } else {
            return null;
        }
    }
    public String testContractWrite(final String contractAddress)
            throws IOException, InterruptedException {

        //only need this if I am using new version of ethj
        final ECKey senderKey = ECKey.fromPrivate(decode(privateKey));

        CompilationResult.ContractMetadata contractBinary = solidityCompilerService.compileContract();
        CallTransaction.Contract compiledContract = new CallTransaction.Contract(contractBinary.abi);

        CallTransaction.Function function = compiledContract.getByName("set");
        byte[] functionCallBytes = function.encode(515);

        Transaction transaction =
                sendTransaction(contractAddress, senderKey, ETHER_TO_TRANSFER_IN_WEI, functionCallBytes);
        return toHexString(transaction.getHash());
    }

    public Object testContractRead(final String contractAddress) throws IOException {
        CompilationResult.ContractMetadata contractBinary;
        contractBinary = solidityCompilerService.compileContract();

        CallTransaction.Contract compiledContract = new CallTransaction.Contract(contractBinary.abi);
        CallTransaction.Function function = compiledContract.getByName("get");

        ProgramResult result;
        result = getEthereum().callConstantFunction(contractAddress, function);
        Object[] functionResult = function.decodeResult(result.getHReturn());
        return functionResult[0];
    }

    public Map<String, String> sendContractToNetwork(final String walletSignature)
            throws IOException, InterruptedException {
        LOGGER.info("Compiling contract...");
        final ECKey senderKey = ECKey.fromPrivate(decode(walletSignature));
        //Here I am compiling the contract in order to obtain the binary.
        CompilationResult.ContractMetadata contractBinary = solidityCompilerService.compileContract();

        LOGGER.info("Preparing transaction to send contract to the network.");
        Transaction contractTransaction =
                sendTransaction(null, senderKey, ETHER_TO_TRANSFER_IN_WEI, decode(contractBinary.bin));

        if (contractTransaction != null) {
            Map<String, String> transactionsData = new HashMap<>();
            transactionsData.put("contractAddress", toHexString(contractTransaction.getContractAddress()));
            return transactionsData;
        } else {
            return null;
        }
    }

    public String writeToContract(final String functionName,
                                  final String walletSignature,
                                  final String contractAddress, final Object... args)
            throws IOException, InterruptedException {
        final ECKey senderKey = ECKey.fromPrivate(decode(walletSignature));

        ////TODO Should only have to compile once then save off to DB
        CompilationResult.ContractMetadata contractBinary = solidityCompilerService.compileContract();
        CallTransaction.Contract compiledContract = new CallTransaction.Contract(contractBinary.abi);

        CallTransaction.Function function = compiledContract.getByName(functionName);
        byte[] functionCallBytes = function.encode(args);

        Transaction transaction =
                sendTransaction(contractAddress, senderKey, ETHER_TO_TRANSFER_IN_WEI, functionCallBytes);
        return toHexString(transaction.getHash());
    }

    public Object[] readContract(final String functionName,
                                 final String contractAddress, final Object... args) throws IOException {
        CompilationResult.ContractMetadata contractBinary = solidityCompilerService.compileContract();
        CallTransaction.Contract compiledContract = new CallTransaction.Contract(contractBinary.abi);

        CallTransaction.Function function = compiledContract.getByName(functionName);

        ProgramResult result;
        if (args != null) {
            result = getEthereum().callConstantFunction(contractAddress, function, args);
        } else {
            result = getEthereum().callConstantFunction(contractAddress, function);
        }
        Object[] functionResult = function.decodeResult(result.getHReturn());
        return functionResult;
    }



    private Transaction getTransaction(final String recipientWalletAddress, final long value,
                                       final byte[] data, final BigInteger nonce) {
        return new Transaction(
                ByteUtil.bigIntegerToBytes(nonce),
                ByteUtil.longToBytesNoLeadZeroes(getEthereum().getGasPrice()),
                ByteUtil.longToBytesNoLeadZeroes(GAS_LIMIT),
                recipientWalletAddress == null ? new byte[0] : decode(recipientWalletAddress),
                ByteUtil.longToBytesNoLeadZeroes(value),
                data,
                getEthereum().getChainIdForNextBlock());
    }


    private Transaction sendTransaction(final String recipientWalletAddress, final ECKey senderKey, final long value,
                                        final byte[] data)
            throws InterruptedException {
        BigInteger nonce = getEthereum().getRepository().getNonce(senderKey.getAddress());
        LOGGER.info("Sender's Address {}", toHexString(senderKey.getAddress()));
        //Creating the transaction to send.
        Transaction transaction = getTransaction(recipientWalletAddress, value, data, nonce);
        //Sign the transaction by the sender.
        transaction.sign(senderKey);

        String transactionHash = toHexString(transaction.getHash());
        LOGGER.info("Transaction created : {}", transactionHash);
        LOGGER.info("Sending transaction to the network ==> {}", transaction);
        getEthereum().submitTransaction(transaction);
        Thread.sleep(TRANSACTION_THREAD_SLEEP);
        return transaction;
    }

    protected void onSyncDone() {
        LOGGER.info("onSyncDone() called.");
        setSynced(true);
    }

    /**
     * We can see the state changes of accounts after the transaction has been sent.
     *
     * @param transaction
     */
    private void onPendingTransactionReceived(final Transaction transaction) {
        LOGGER.info("onPendingTransactionReceived: {}", transaction);
        pendingTransactions.put(new ByteArrayWrapper(transaction.getHash()), transaction);
    }

    /**
     * Each block received from the network is checked for out pending transactions.
     * If the block contains our transactions then they will be removed from pending transactions.
     *
     * @param block    - Incoming block from the network.
     * @param receipts - transaction receipts from the mined transactions.
     */
    private void onBlock(final Block block, final List<TransactionReceipt> receipts) {
        LOGGER.info("Status of block, number of transactions included: {}", block.getTransactionsList().size());
        LOGGER.info("Block number: {}", block.getNumber());
        for (Transaction transaction : block.getTransactionsList()) {
            ByteArrayWrapper txHash = new ByteArrayWrapper(transaction.getHash());
            Transaction ptx = pendingTransactions.get(txHash);
            if (ptx != null) {
                LOGGER.info(" - Pending transaction cleared 0x{} in block {}",
                        toHexString(transaction.getHash()).substring(0, 8), block.getShortDescr());
                pendingTransactions.remove(txHash);
            }
        }
    }
}
