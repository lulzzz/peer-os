/* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package org.safehaus.kiskis.mgmt.api.taskrunner;

import java.util.ArrayList;
import java.util.List;

/**
 * This is just a utility class used to group tasks into one sequence of tasks
 * related to each other -> operation
 *
 * @author dilshat
 */
public class Operation {

    private int currentTaskId = -1;
    private final List<Task> tasks;
    private final String description;

    public Operation(String description) {
        this.description = description;
        tasks = new ArrayList<Task>();
    }

    public String getDescription() {
        return description;
    }

    public int getTotalTimeout() {
        int timeout = 0;
        for (Task task : tasks) {
            timeout += task.getAvgTimeout();
        }

        return timeout;
    }

    protected void addTask(Task task) {
        if (task != null) {
            tasks.add(task);
        }
    }

    public Task getNextTask() {
        if (hasNextTask()) {
            return tasks.get(++currentTaskId);
        }

        return null;
    }

    public Task peekNextTask() {
        if (hasNextTask()) {
            return tasks.get(currentTaskId + 1);
        }
        return null;
    }

    public Task peekPreviousTask() {
        if (currentTaskId > 0) {
            return tasks.get(currentTaskId - 1);
        }
        return null;
    }

    public Task peekCurrentTask() {
        if (currentTaskId >= 0) {
            return tasks.get(currentTaskId);
        }
        return null;
    }

    public boolean hasNextTask() {
        return currentTaskId < tasks.size() - 1;
    }

}
