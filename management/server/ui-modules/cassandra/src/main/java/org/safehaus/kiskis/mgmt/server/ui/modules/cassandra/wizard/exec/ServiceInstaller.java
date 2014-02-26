/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.wizard.exec;

import org.safehaus.kiskis.mgmt.server.command.RequestUtil;
import com.vaadin.ui.TextArea;
import org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.commands.CassandraCommands;
import org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.wizard.CassandraConfig;
import org.safehaus.kiskis.mgmt.shared.protocol.*;
import org.safehaus.kiskis.mgmt.shared.protocol.api.CommandManager;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.TaskStatus;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.CassandraDAO;
import org.safehaus.kiskis.mgmt.server.ui.modules.cassandra.management.CassandraCommandEnum;
import org.safehaus.kiskis.mgmt.shared.protocol.api.Command;

/**
 *
 * @author bahadyr
 */
public class ServiceInstaller {

    private final Queue<Task> tasks = new LinkedList<Task>();
    private final TextArea terminal;
    private Task currentTask;
    CassandraConfig config;

    public ServiceInstaller(CassandraConfig config, TextArea terminal) {
        this.terminal = terminal;
        this.config = config;

        Task updateApt = RequestUtil.createTask("apt-get update");
        for (Agent agent : config.getSelectedAgents()) {
            Command command = CassandraCommands.getAptGetUpdate();
            command.getRequest().setUuid(agent.getUuid());
            command.getRequest().setTaskUuid(updateApt.getUuid());
            command.getRequest().setRequestSequenceNumber(updateApt.getIncrementedReqSeqNumber());
            updateApt.addCommand(command);
        }
        tasks.add(updateApt);

        Task installTask = RequestUtil.createTask("Install Cassandra");
        for (Agent agent : config.getSelectedAgents()) {
            Command command = new CassandraCommands().getCommand(CassandraCommandEnum.INSTALL);
            command.getRequest().setUuid(agent.getUuid());
            command.getRequest().setTaskUuid(installTask.getUuid());
            command.getRequest().setRequestSequenceNumber(installTask.getIncrementedReqSeqNumber());
            installTask.addCommand(command);
        }
        tasks.add(installTask);

        Task sourceEtcProfileTask = RequestUtil.createTask("Update profile");
        for (Agent agent : config.getSelectedAgents()) {
            Command sourceEtcProfileCommand = CassandraCommands.getSourceEtcProfileUpdateCommand();
            sourceEtcProfileCommand.getRequest().setUuid(agent.getUuid());
            sourceEtcProfileCommand.getRequest().setTaskUuid(sourceEtcProfileTask.getUuid());
            sourceEtcProfileCommand.getRequest().setRequestSequenceNumber(sourceEtcProfileTask.getIncrementedReqSeqNumber());
            sourceEtcProfileTask.addCommand(sourceEtcProfileCommand);
        }
        tasks.add(sourceEtcProfileTask);

        Task setListenAddressTask = RequestUtil.createTask("Set listen addresses");
        for (Agent agent : config.getSelectedAgents()) {
            Command setListenAddressCommand = CassandraCommands.getSetListenAddressCommand(agent.getHostname() + "." + config.getDomainName());
            setListenAddressCommand.getRequest().setUuid(agent.getUuid());
            setListenAddressCommand.getRequest().setTaskUuid(setListenAddressTask.getUuid());
            setListenAddressCommand.getRequest().setRequestSequenceNumber(setListenAddressTask.getIncrementedReqSeqNumber());
            setListenAddressTask.addCommand(setListenAddressCommand);

            Command setRpcAddressCommand = CassandraCommands.getSetRpcAddressCommand(agent.getHostname() + "." + config.getDomainName());
            setRpcAddressCommand.getRequest().setUuid(agent.getUuid());
            setRpcAddressCommand.getRequest().setTaskUuid(setListenAddressTask.getUuid());
            setRpcAddressCommand.getRequest().setRequestSequenceNumber(setListenAddressTask.getIncrementedReqSeqNumber());
            setListenAddressTask.addCommand(setRpcAddressCommand);

        }
        tasks.add(setListenAddressTask);

        Task setSeedsTask = RequestUtil.createTask("Set seeds addresses");
        StringBuilder seedsSB = new StringBuilder();
        for (Agent agent : config.getSeeds()) {
            seedsSB.append(agent.getHostname()).append(".").append(config.getDomainName()).append(",");
        }
        for (Agent agent : config.getSelectedAgents()) {
            Command setSeedsCommand = CassandraCommands.getSetSeedsCommand(seedsSB.substring(0, seedsSB.length() - 1));
            setSeedsCommand.getRequest().setUuid(agent.getUuid());
            setSeedsCommand.getRequest().setTaskUuid(setSeedsTask.getUuid());
            setSeedsCommand.getRequest().setRequestSequenceNumber(setSeedsTask.getIncrementedReqSeqNumber());
            setSeedsTask.addCommand(setSeedsCommand);
        }
        tasks.add(setSeedsTask);

        if (!config.getClusterName().isEmpty()) {
            Task clusterRenameTask = RequestUtil.createTask("Rename cluster");
            for (Agent agent : config.getSelectedAgents()) {
                Command setClusterNameCommand = CassandraCommands.getSetClusterNameCommand(config.getClusterName());
                setClusterNameCommand.getRequest().setUuid(agent.getUuid());
                setClusterNameCommand.getRequest().setTaskUuid(clusterRenameTask.getUuid());
                setClusterNameCommand.getRequest().setRequestSequenceNumber(clusterRenameTask.getIncrementedReqSeqNumber());
                clusterRenameTask.addCommand(setClusterNameCommand);

                Command deleteDataDir = CassandraCommands.getDeleteDataDirectoryCommand();
                deleteDataDir.getRequest().setUuid(agent.getUuid());
                deleteDataDir.getRequest().setTaskUuid(clusterRenameTask.getUuid());
                deleteDataDir.getRequest().setRequestSequenceNumber(clusterRenameTask.getIncrementedReqSeqNumber());
                clusterRenameTask.addCommand(deleteDataDir);

                Command deleteCommitLogDit = CassandraCommands.getDeleteCommitLogDirectoryCommand();
                deleteCommitLogDit.getRequest().setUuid(agent.getUuid());
                deleteCommitLogDit.getRequest().setTaskUuid(clusterRenameTask.getUuid());
                deleteCommitLogDit.getRequest().setRequestSequenceNumber(clusterRenameTask.getIncrementedReqSeqNumber());
                clusterRenameTask.addCommand(deleteCommitLogDit);

                Command deleteSavedCashesDir = CassandraCommands.getDeleteSavedCachesDirectoryCommand();
                deleteSavedCashesDir.getRequest().setUuid(agent.getUuid());
                deleteSavedCashesDir.getRequest().setTaskUuid(clusterRenameTask.getUuid());
                deleteSavedCashesDir.getRequest().setRequestSequenceNumber(clusterRenameTask.getIncrementedReqSeqNumber());
                clusterRenameTask.addCommand(deleteSavedCashesDir);

            }
            tasks.add(clusterRenameTask);
        }

        if (!config.getDataDirectory().isEmpty()) {
            Task setDataDirectory = RequestUtil.createTask("Change data directory");
            for (Agent agent : config.getSelectedAgents()) {
                Command setDataDir = CassandraCommands.getSetDataDirectoryCommand(config.getDataDirectory());
                setDataDir.getRequest().setUuid(agent.getUuid());
                setDataDir.getRequest().setTaskUuid(setDataDirectory.getUuid());
                setDataDir.getRequest().setRequestSequenceNumber(setDataDirectory.getIncrementedReqSeqNumber());
                setDataDirectory.addCommand(setDataDir);

            }
            tasks.add(setDataDirectory);
        }

        if (!config.getCommitLogDirectory().isEmpty()) {
            Task setCommitLogDirectoryTask = RequestUtil.createTask("Change Commit log directory");
            for (Agent agent : config.getSelectedAgents()) {
                Command setCommitLogDir = CassandraCommands.getSetCommitLogDirectoryCommand(config.getCommitLogDirectory());
                setCommitLogDir.getRequest().setUuid(agent.getUuid());
                setCommitLogDir.getRequest().setTaskUuid(setCommitLogDirectoryTask.getUuid());
                setCommitLogDir.getRequest().setRequestSequenceNumber(setCommitLogDirectoryTask.getIncrementedReqSeqNumber());
                setCommitLogDirectoryTask.addCommand(setCommitLogDir);
            }
            tasks.add(setCommitLogDirectoryTask);
        }

        if (!config.getSavedCachesDirectory().isEmpty()) {
            Task setSavedCashesDirectoryTask = RequestUtil.createTask("Change Saved caches directory");
            for (Agent agent : config.getSelectedAgents()) {
                Command setSavedCachesDir = CassandraCommands.getSetSavedCachesDirectoryCommand(config.getSavedCachesDirectory());
                setSavedCachesDir.getRequest().setUuid(agent.getUuid());
                setSavedCachesDir.getRequest().setTaskUuid(setSavedCashesDirectoryTask.getUuid());
                setSavedCachesDir.getRequest().setRequestSequenceNumber(setSavedCashesDirectoryTask.getIncrementedReqSeqNumber());
                setSavedCashesDirectoryTask.addCommand(setSavedCachesDir);

            }
            tasks.add(setSavedCashesDirectoryTask);
        }
    }

    public void start() {
        terminal.setValue("Cassandra cluster installation started...\n");
        moveToNextTask();
        if (currentTask != null) {
            for (Command command : currentTask.getCommands()) {
                executeCommand(command);
            }
        }
    }

    private void moveToNextTask() {
        currentTask = tasks.poll();
    }

    public void onResponse(Response response) {
        if (currentTask != null && response.getTaskUuid() != null
                && currentTask.getUuid().compareTo(response.getTaskUuid()) == 0) {

            List<ParseResult> list = RequestUtil.parseTask(response.getTaskUuid(), true);
            Task task = RequestUtil.getTask(response.getTaskUuid());
            if (!list.isEmpty() && terminal != null) {
                if (task.getTaskStatus() == TaskStatus.SUCCESS) {
                    terminal.setValue(terminal.getValue().toString() + task.getDescription() + " successfully finished.\n");
                    moveToNextTask();
                    if (currentTask != null) {
                        terminal.setValue(terminal.getValue().toString() + "Running next step " + currentTask.getDescription() + "\n");
                        for (Command command : currentTask.getCommands()) {
                            executeCommand(command);
                        }
                    } else {
                        terminal.setValue(terminal.getValue().toString() + "Tasks complete.\n");
                        saveCCI();
                    }
                } else if (task.getTaskStatus() == TaskStatus.FAIL) {
                    terminal.setValue(terminal.getValue().toString() + task.getDescription() + " failed\n");
                }
            }
            terminal.setCursorPosition(terminal.getValue().toString().length());

        }
    }

    private void saveCCI() {
        CassandraClusterInfo cci = new CassandraClusterInfo();
        cci.setName(config.getClusterName());
        cci.setDataDir(config.getDataDirectory());
        cci.setCommitLogDir(config.getCommitLogDirectory());
        cci.setSavedCacheDir(config.getSavedCachesDirectory());
        cci.setSeeds(config.getSeedsUUIDList());
        cci.setNodes(config.getAgentsUUIDList());
        cci.setDomainName(config.getDomainName());

        if (CassandraDAO.saveCassandraClusterInfo(cci)) {
            terminal.setValue(terminal.getValue().toString() + cci.getUuid() + " cluster saved into keyspace.\n");
        }
    }

    private void executeCommand(Command command) {
        terminal.setValue(terminal.getValue() + command.getRequest().getProgram() + "\n");
        ServiceLocator.getService(CommandManager.class).executeCommand(command);
    }

}
