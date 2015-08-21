package io.subutai.core.peer.cli;


import java.util.List;

import io.subutai.common.peer.Peer;
import io.subutai.common.peer.PeerException;
import io.subutai.core.identity.rbac.cli.SubutaiShellCommandSupport;
import io.subutai.core.peer.api.PeerManager;

import org.apache.karaf.shell.commands.Command;


@Command( scope = "peer", name = "ls" )
public class ListCommand extends SubutaiShellCommandSupport
{

    private PeerManager peerManager;


    public void setPeerManager( final PeerManager peerManager )
    {
        this.peerManager = peerManager;
    }


    @Override
    protected Object doExecute() throws Exception
    {
        List<Peer> list = peerManager.getPeers();
        System.out.println( "Found " + list.size() + " registered peers" );
        for ( Peer peer : list )
        {
            String peerStatus = "OFFLINE";
            try
            {

                if ( peer.isOnline() )
                {
                    peerStatus = "ONLINE";
                }
            }
            catch ( PeerException pe )
            {
                peerStatus += " " + pe.getMessage();
            }
            System.out.println(
                    peer.getId() + " " + peer.getPeerInfo().getIp() + " " + peer.getName() + " " + peerStatus );
        }
        return null;
    }
}