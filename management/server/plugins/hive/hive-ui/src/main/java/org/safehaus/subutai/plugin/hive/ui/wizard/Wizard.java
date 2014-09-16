package org.safehaus.subutai.plugin.hive.ui.wizard;


import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

import org.safehaus.subutai.common.util.ServiceLocator;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.hadoop.api.Hadoop;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.hive.api.Hive;
import org.safehaus.subutai.plugin.hive.api.HiveConfig;

import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;


public class Wizard {

    private final GridLayout grid;
    private int step = 1;
    private HiveConfig config = new HiveConfig();
    private HadoopClusterConfig hadoopConfig = new HadoopClusterConfig();
    private final Hive hive;
    private final Hadoop hadoop;
    private final ExecutorService executorService;
    private final Tracker tracker;


    public Wizard( ExecutorService executorService, ServiceLocator serviceLocator ) throws NamingException {

        this.executorService = executorService;
        this.hadoop = serviceLocator.getService( Hadoop.class );
        this.tracker = serviceLocator.getService( Tracker.class );
        this.hive = serviceLocator.getService( Hive.class );

        grid = new GridLayout( 1, 20 );
        grid.setMargin( true );
        grid.setSizeFull();

        putForm();
    }


    private void putForm() {
        grid.removeComponent( 0, 1 );
        Component component = null;
        switch ( step ) {
            case 1: {
                component = new WelcomeStep( this );
                break;
            }
            case 2: {
                component = new NodeSelectionStep( hive, hadoop, this );
                break;
            }
            case 3: {
                component = new VerificationStep( hive, executorService, tracker, this );
                break;
            }
            default: {
                break;
            }
        }

        if ( component != null ) {
            grid.addComponent( component, 0, 1, 0, 19 );
        }
    }


    public Component getContent() {
        return grid;
    }


    protected void next() {
        step++;
        putForm();
    }


    protected void back() {
        step--;
        putForm();
    }


    protected void init() {
        step = 1;
        config = new HiveConfig();
        hadoopConfig = new HadoopClusterConfig();
        putForm();
    }


    public HiveConfig getConfig() {
        return config;
    }


    public HadoopClusterConfig getHadoopConfig() {
        return hadoopConfig;
    }
}
