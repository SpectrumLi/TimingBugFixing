/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.hadoop.mapred;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.ipc.ProtocolSignature;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.mapred.SortedRanges.Range;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.TypeConverter;
import org.apache.hadoop.mapreduce.security.token.JobTokenSecretManager;
import org.apache.hadoop.mapreduce.v2.api.records.TaskType;
import org.apache.hadoop.mapreduce.v2.app.AppContext;
import org.apache.hadoop.mapreduce.v2.app.TaskAttemptListener;
import org.apache.hadoop.mapreduce.v2.app.TaskHeartbeatHandler;
import org.apache.hadoop.mapreduce.v2.app.job.Job;
import org.apache.hadoop.mapreduce.v2.app.job.Task;
import org.apache.hadoop.mapreduce.v2.app.job.event.TaskAttemptDiagnosticsUpdateEvent;
import org.apache.hadoop.mapreduce.v2.app.job.event.TaskAttemptEvent;
import org.apache.hadoop.mapreduce.v2.app.job.event.TaskAttemptEventType;
import org.apache.hadoop.mapreduce.v2.app.job.event.TaskAttemptStatusUpdateEvent;
import org.apache.hadoop.mapreduce.v2.app.job.event.TaskAttemptStatusUpdateEvent.TaskAttemptStatus;
import org.apache.hadoop.mapreduce.v2.app.security.authorize.MRAMPolicyProvider;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.authorize.PolicyProvider;
import org.apache.hadoop.yarn.YarnException;
import org.apache.hadoop.yarn.service.CompositeService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is responsible for talking to the task umblical.
 * It also converts all the old data structures
 * to yarn data structures.
 * 
 * This class HAS to be in this package to access package private 
 * methods/classes.
 */
public class TaskAttemptListenerImpl extends CompositeService implements TaskUmbilicalProtocol, TaskAttemptListener {

    private static final Log LOG = LogFactory.getLog(TaskAttemptListenerImpl.class);

    public static int dfixflag = 1;

    private AppContext context;

    private Server server;

    private TaskHeartbeatHandler taskHeartbeatHandler;

    private InetSocketAddress address;

    private Map<WrappedJvmID, org.apache.hadoop.mapred.Task> jvmIDToAttemptMap = Collections.synchronizedMap(new HashMap<WrappedJvmID, org.apache.hadoop.mapred.Task>());

    private JobTokenSecretManager jobTokenSecretManager = null;

    public TaskAttemptListenerImpl(AppContext context, JobTokenSecretManager jobTokenSecretManager) {
        super(TaskAttemptListenerImpl.class.getName());
        this.context = context;
        this.jobTokenSecretManager = jobTokenSecretManager;
    }

    @Override
    public void init(Configuration conf) {
        registerHeartbeatHandler();
        super.init(conf);
    }

    @Override
    public void start() {
        startRpcServer();
        super.start();
    }

    protected void registerHeartbeatHandler() {
        taskHeartbeatHandler = new TaskHeartbeatHandler(context.getEventHandler(), context.getClock());
        addService(taskHeartbeatHandler);
    }

    protected void startRpcServer() {
        Configuration conf = getConfig();
        try {
            server = RPC.getServer(TaskUmbilicalProtocol.class, this, "0.0.0.0", 0, conf.getInt(MRJobConfig.MR_AM_TASK_LISTENER_THREAD_COUNT, MRJobConfig.DEFAULT_MR_AM_TASK_LISTENER_THREAD_COUNT), false, conf, jobTokenSecretManager);
            // Enable service authorization?
            if (conf.getBoolean(CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHORIZATION, false)) {
                refreshServiceAcls(conf, new MRAMPolicyProvider());
            }
            server.start();
            InetSocketAddress listenerAddress = server.getListenerAddress();
            this.address = NetUtils.createSocketAddr(listenerAddress.getAddress().getLocalHost().getCanonicalHostName() + ":" + listenerAddress.getPort());
        } catch (IOException e) {
            throw new YarnException(e);
        }
    }

    void refreshServiceAcls(Configuration configuration, PolicyProvider policyProvider) {
        this.server.refreshServiceAcl(configuration, policyProvider);
    }

    @Override
    public void stop() {
        stopRpcServer();
        super.stop();
    }

    protected void stopRpcServer() {
        server.stop();
    }

    @Override
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
   * Child checking whether it can commit.
   * 
   * <br/>
   * Commit is a two-phased protocol. First the attempt informs the
   * ApplicationMaster that it is
   * {@link #commitPending(TaskAttemptID, TaskStatus)}. Then it repeatedly polls
   * the ApplicationMaster whether it {@link #canCommit(TaskAttemptID)} This is
   * a legacy from the centralized commit protocol handling by the JobTracker.
   */
    @Override
    public //public boolean canCommit(TaskAttemptID taskAttemptID) throws IOException {
    boolean canCommit(TaskAttemptID taskAttemptID, String DMID) throws IOException {
        LOG.info("Commit go/no-go request from " + taskAttemptID.toString());
        // An attempt is asking if it can commit its output. This can be decided
        // only by the task which is managing the multiple attempts. So redirect the
        // request there.
        org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID = TypeConverter.toYarn(taskAttemptID);
        taskHeartbeatHandler.receivedPing(attemptID);
        Job job = context.getJob(attemptID.getTaskId().getJobId());
        Task task = job.getTask(attemptID.getTaskId());
        return task.canCommit(attemptID);
    }

    /**
   * TaskAttempt is reporting that it is in commit_pending and it is waiting for
   * the commit Response
   * 
   * <br/>
   * Commit it a two-phased protocol. First the attempt informs the
   * ApplicationMaster that it is
   * {@link #commitPending(TaskAttemptID, TaskStatus)}. Then it repeatedly polls
   * the ApplicationMaster whether it {@link #canCommit(TaskAttemptID)} This is
   * a legacy from the centralized commit protocol handling by the JobTracker.
   */
    @Override
    public //public void commitPending(TaskAttemptID taskAttemptID, TaskStatus taskStatsu)
    void commitPending(TaskAttemptID taskAttemptID, TaskStatus taskStatsu, String DMID) throws IOException, InterruptedException {
        LOG.info("Commit-pending state update from " + taskAttemptID.toString());
        // An attempt is asking if it can commit its output. This can be decided
        // only by the task which is managing the multiple attempts. So redirect the
        // request there.
        org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID = TypeConverter.toYarn(taskAttemptID);
        taskHeartbeatHandler.receivedPing(attemptID);
        //Ignorable TaskStatus? - since a task will send a LastStatusUpdate
        context.getEventHandler().handle(new TaskAttemptEvent(attemptID, TaskAttemptEventType.TA_COMMIT_PENDING));
    }

    @Override
    public //public void done(TaskAttemptID taskAttemptID) throws IOException {
    void done(TaskAttemptID taskAttemptID, String DMID) throws IOException {
        LOG.info("Done acknowledgement from " + taskAttemptID.toString());
        org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID = TypeConverter.toYarn(taskAttemptID);
        taskHeartbeatHandler.receivedPing(attemptID);
        context.getEventHandler().handle(new TaskAttemptEvent(attemptID, TaskAttemptEventType.TA_DONE));
    }

    @Override
    public //public void fatalError(TaskAttemptID taskAttemptID, String msg)
    void fatalError(TaskAttemptID taskAttemptID, String msg, String DMID) throws IOException {
        // This happens only in Child and in the Task.
        LOG.fatal("Task: " + taskAttemptID + " - exited : " + msg);
        //reportDiagnosticInfo(taskAttemptID, "Error: " + msg);
        reportDiagnosticInfo(taskAttemptID, "Error: " + msg, "0");
        org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID = TypeConverter.toYarn(taskAttemptID);
        context.getEventHandler().handle(new TaskAttemptEvent(attemptID, TaskAttemptEventType.TA_FAILMSG));
    }

    @Override
    public //public void fsError(TaskAttemptID taskAttemptID, String message)
    void fsError(TaskAttemptID taskAttemptID, String message, String DMID) throws IOException {
        // This happens only in Child.
        LOG.fatal("Task: " + taskAttemptID + " - failed due to FSError: " + message);
        //reportDiagnosticInfo(taskAttemptID, "FSError: " + message);
        reportDiagnosticInfo(taskAttemptID, "FSError: " + message, "0");
        org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID = TypeConverter.toYarn(taskAttemptID);
        context.getEventHandler().handle(new TaskAttemptEvent(attemptID, TaskAttemptEventType.TA_FAILMSG));
    }

    @Override
    public void shuffleError(TaskAttemptID taskAttemptID, String message) throws IOException {
    // TODO: This isn't really used in any MR code. Ask for removal.    
    }

    @Override
    public MapTaskCompletionEventsUpdate getMapCompletionEvents(JobID jobIdentifier, int fromEventId, int maxEvents, //TaskAttemptID taskAttemptID) throws IOException {
    TaskAttemptID taskAttemptID, String DMID) throws IOException {
        LOG.info("MapCompletionEvents request from " + taskAttemptID.toString() + ". fromEventID " + fromEventId + " maxEvents " + maxEvents);
        // TODO: shouldReset is never used. See TT. Ask for Removal.
        boolean shouldReset = false;
        org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID = TypeConverter.toYarn(taskAttemptID);
        org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptCompletionEvent[] events = context.getJob(attemptID.getTaskId().getJobId()).getTaskAttemptCompletionEvents(fromEventId, maxEvents);
        taskHeartbeatHandler.receivedPing(attemptID);
        // filter the events to return only map completion events in old format
        List<TaskCompletionEvent> mapEvents = new ArrayList<TaskCompletionEvent>();
        for (org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptCompletionEvent event : events) {
            if (TaskType.MAP.equals(event.getAttemptId().getTaskId().getTaskType())) {
                mapEvents.add(TypeConverter.fromYarn(event));
            }
        }
        return new MapTaskCompletionEventsUpdate(mapEvents.toArray(new TaskCompletionEvent[0]), shouldReset);
    }

    @Override
    public //public boolean ping(TaskAttemptID taskAttemptID) throws IOException {
    boolean ping(TaskAttemptID taskAttemptID, String DMID) throws IOException {
        LOG.info("Ping from " + taskAttemptID.toString());
        taskHeartbeatHandler.receivedPing(TypeConverter.toYarn(taskAttemptID));
        return true;
    }

    @Override
    public //public void reportDiagnosticInfo(TaskAttemptID taskAttemptID, String diagnosticInfo)
    void reportDiagnosticInfo(TaskAttemptID taskAttemptID, String diagnosticInfo, String DMID) throws IOException {
        LOG.info("Diagnostics report from " + taskAttemptID.toString() + ": " + diagnosticInfo);
        org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID = TypeConverter.toYarn(taskAttemptID);
        taskHeartbeatHandler.receivedPing(attemptID);
        // This is mainly used for cases where we want to propagate exception traces
        // of tasks that fail.
        // This call exists as a hadoop mapreduce legacy wherein all changes in
        // counters/progress/phase/output-size are reported through statusUpdate()
        // call but not diagnosticInformation.
        context.getEventHandler().handle(new TaskAttemptDiagnosticsUpdateEvent(attemptID, diagnosticInfo));
    }

    @Override
    public boolean statusUpdate(TaskAttemptID taskAttemptID, //TaskStatus taskStatus) throws IOException, InterruptedException {
    TaskStatus taskStatus, String DMID) throws IOException, InterruptedException {
        LOG.info("Status update from " + taskAttemptID.toString());
        org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId yarnAttemptID = TypeConverter.toYarn(taskAttemptID);
        taskHeartbeatHandler.receivedPing(yarnAttemptID);
        TaskAttemptStatus taskAttemptStatus = new TaskAttemptStatus();
        taskAttemptStatus.id = yarnAttemptID;
        // Task sends the updated progress to the TT.
        taskAttemptStatus.progress = taskStatus.getProgress();
        LOG.info("Progress of TaskAttempt " + taskAttemptID + " is : " + taskStatus.getProgress());
        // Task sends the updated state-string to the TT.
        taskAttemptStatus.stateString = taskStatus.getStateString();
        // Set the output-size when map-task finishes. Set by the task itself.
        taskAttemptStatus.outputSize = taskStatus.getOutputSize();
        // Task sends the updated phase to the TT.
        taskAttemptStatus.phase = TypeConverter.toYarn(taskStatus.getPhase());
        // Counters are updated by the task.
        taskAttemptStatus.counters = TypeConverter.toYarn(taskStatus.getCounters());
        // Map Finish time set by the task (map only)
        if (taskStatus.getIsMap() && taskStatus.getMapFinishTime() != 0) {
            taskAttemptStatus.mapFinishTime = taskStatus.getMapFinishTime();
        }
        // Shuffle Finish time set by the task (reduce only).
        if (!taskStatus.getIsMap() && taskStatus.getShuffleFinishTime() != 0) {
            taskAttemptStatus.shuffleFinishTime = taskStatus.getShuffleFinishTime();
        }
        // Sort finish time set by the task (reduce only).
        if (!taskStatus.getIsMap() && taskStatus.getSortFinishTime() != 0) {
            taskAttemptStatus.sortFinishTime = taskStatus.getSortFinishTime();
        }
        //set the fetch failures
        if (taskStatus.getFetchFailedMaps() != null && taskStatus.getFetchFailedMaps().size() > 0) {
            taskAttemptStatus.fetchFailedMaps = new ArrayList<org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId>();
            for (TaskAttemptID failedMapId : taskStatus.getFetchFailedMaps()) {
                taskAttemptStatus.fetchFailedMaps.add(TypeConverter.toYarn(failedMapId));
            }
        }
        // Task sends the information about the nextRecordRange to the TT
        //    TODO: The following are not needed here, but needed to be set somewhere inside AppMaster.
        //    taskStatus.getRunState(); // Set by the TT/JT. Transform into a state TODO
        //    taskStatus.getStartTime(); // Used to be set by the TaskTracker. This should be set by getTask().
        //    taskStatus.getFinishTime(); // Used to be set by TT/JT. Should be set when task finishes
        //    // This was used by TT to do counter updates only once every minute. So this
        //    // isn't ever changed by the Task itself.
        //    taskStatus.getIncludeCounters();
        context.getEventHandler().handle(new TaskAttemptStatusUpdateEvent(taskAttemptStatus.id, taskAttemptStatus));
        return true;
    }

    @Override
    public long getProtocolVersion(String arg0, long arg1) throws IOException {
        return TaskUmbilicalProtocol.versionID;
    }

    @Override
    public //public void reportNextRecordRange(TaskAttemptID taskAttemptID, Range range)
    void reportNextRecordRange(TaskAttemptID taskAttemptID, Range range, String DMID) throws IOException {
        // call but not the next record range information.
        throw new IOException("Not yet implemented.");
    }

    @Override
    public //public JvmTask getTask(JvmContext context) throws IOException {
    JvmTask getTask(JvmContext context, String DMID) throws IOException {
        // A rough imitation of code from TaskTracker.
        JVMId jvmId = context.jvmId;
        LOG.info("JVM with ID : " + jvmId + " asked for a task");
        // TODO: Is it an authorised container to get a task? Otherwise return null.
        // TODO: Is the request for task-launch still valid?
        // TODO: Child.java's firstTaskID isn't really firstTaskID. Ask for update
        // to jobId and task-type.
        WrappedJvmID wJvmID = new WrappedJvmID(jvmId.getJobId(), jvmId.isMap, jvmId.getId());
        org.apache.hadoop.mapred.Task task = jvmIDToAttemptMap.get(wJvmID);DFix.Signal(jvmIDToAttemptMap,wJvmID);

        if (task != null) {
            //there may be lag in the attempt getting added here
            LOG.info("JVM with ID: " + jvmId + " given task: " + task.getTaskID());
            JvmTask jvmTask = new JvmTask(task, false);
            //remove the task as it is no more needed and free up the memory
            jvmIDToAttemptMap.remove(wJvmID);
            dfixflag = 0;
            LOG.info("getTask changes dfixflag to " + dfixflag);
            return jvmTask;
        }
        return null;
    }

    @Override
    public void register(org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID, org.apache.hadoop.mapred.Task task, WrappedJvmID jvmID) {
        //create the mapping so that it is easy to look up
        //when it comes back to ask for Task.
        jvmIDToAttemptMap.put(jvmID, task);
        //register this attempt
        taskHeartbeatHandler.register(attemptID);
    }

    @Override
    public void unregister_dfix(org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID, WrappedJvmID jvmID) throws Exception {
        StackTraceElement[] cause = Thread.currentThread().getStackTrace();
        String _str = "";
        for (StackTraceElement sye : cause) {
            _str = _str + sye.getClassName() + "-" + sye.getMethodName() + "-" + sye.getLineNumber() + ";";
        }
        //remove the mapping if not already removed
    if (!DFix.CheckDrop(jvmIDToAttemptMap,wJvmID)) return ;    jvmIDToAttemptMap.remove(jvmID);
        //unregister this attempt
        taskHeartbeatHandler.unregister(attemptID);
    }

    @Override
    public ProtocolSignature getProtocolSignature(String protocol, long clientVersion, int clientMethodsHash) throws IOException {
        return ProtocolSignature.getProtocolSignature(this, protocol, clientVersion, clientMethodsHash);
    }

    @Override
    public void unregister(org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId attemptID, WrappedJvmID jvmID) {
        StackTraceElement[] cause = Thread.currentThread().getStackTrace();
        String _str = "";
        for (StackTraceElement sye : cause) {
            _str = _str + sye.getClassName() + "-" + sye.getMethodName() + "-" + sye.getLineNumber() + ";";
        }
        //remove the mapping if not already removed
        jvmIDToAttemptMap.remove(jvmID);
        //unregister this attempt
        taskHeartbeatHandler.unregister(attemptID);
    }

    public static AtomicInteger dfixeventflag = new AtomicInteger();

    public static HashMap<String, Semaphore> hm_dfix = new HashMap<String, Semaphore>();
}
