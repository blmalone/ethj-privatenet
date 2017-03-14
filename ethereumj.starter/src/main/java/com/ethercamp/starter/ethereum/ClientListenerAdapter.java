package com.ethercamp.starter.ethereum;

/**
 * Created by blainemalone on 13/12/2016.
 */

import org.ethereum.facade.Ethereum;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.net.eth.message.StatusMessage;
import org.ethereum.net.rlpx.Node;
import org.ethereum.net.server.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ClientListenerAdapter extends EthereumListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientListenerAdapter.class);

    private Client client;
    private Ethereum ethereum;

    public ClientListenerAdapter(final Client client) {
        this.client = client;
        this.ethereum = client.getEthereum();
    }

    @Override
    public void onNodeDiscovered(final Node node) {
        client.getNodesDiscovered().add(node);
        LOGGER.info("node discovered: {}", node);
    }

    @Override
    public void onEthStatusUpdated(final Channel channel, final StatusMessage statusMessage) {
        client.getEthNodes().put(channel.getNode(), statusMessage);
    }

    @Override
    public void onPeerAddedToSyncPool(final Channel peer) {
        client.getSyncPeers().add(peer);
        LOGGER.info("peer {} has been added to transaction pool", peer);

    }

    @Override
    public void onSyncDone(SyncState state) {
        client.setSynced(true);
        LOGGER.info("On SyncDone method called!");
    }

}
