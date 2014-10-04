package org.safehaus.subutai.core.environment.api;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.safehaus.subutai.common.protocol.CloneContainersMessage;
import org.safehaus.subutai.core.environment.api.helper.EnvironmentBuildProcess;
import org.safehaus.subutai.core.environment.api.helper.ProcessStatusEnum;

import static org.junit.Assert.assertEquals;


public class EnvironmentBuildProcessTest
{
    private static final String NAME = "name";
    EnvironmentBuildProcess process;


    @Before
    public void setUp() throws Exception
    {
        process = new EnvironmentBuildProcess( NAME );
    }


    @Test
    public void testSetMessageMap() throws Exception
    {
        Map<String, CloneContainersMessage> map = new HashMap<>();
        process.setMessageMap( map );
    }


    @Test
    public void testSetTimestamp() throws Exception
    {
        long t = System.currentTimeMillis();
        process.setTimestamp( t );
        assertEquals( t, process.getTimestamp() );
    }


    @Test
    public void testCompleteStatus() throws Exception
    {
        process.setCompleteStatus( Boolean.TRUE );
    }


    @Test
    public void testUuid() throws Exception
    {
        UUID uuid = UUID.randomUUID();
        process.setUuid( uuid );
        assertEquals( uuid, process.getUuid() );
    }


    @Test
    public void testSetGetProcessStatusEnum() throws Exception
    {
        process.setProcessStatusEnum( ProcessStatusEnum.NEW_PROCESS );
        assertEquals( ProcessStatusEnum.NEW_PROCESS, process.getProcessStatusEnum() );
    }


    @Test
    public void testSetGetEnvironmentName() throws Exception
    {
        process.setEnvironmentName( NAME );
        assertEquals( NAME, process.getEnvironmentName() );
    }
}

