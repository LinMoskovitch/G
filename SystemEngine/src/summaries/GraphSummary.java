package summaries;

import target.Graph;
import target.Target;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class GraphSummary implements Serializable {
    //--------------------------------------------------Members-----------------------------------------------------//
    private final String graphName;
    private Duration totalTime;
    private Instant timeStarted;
    private final Map<String, TargetSummary> targetsSummaryMap;
    private Map<TargetSummary.ResultStatus, Integer> allResultStatus;
    private Boolean firstRun;
    private Integer skippedTargets;
    private final String workingDirectory;
    private Set<String> closedSerialSets;
    private Instant timePaused;
    private Duration totalPausedTime;

    //------------------------------------------------Constructors--------------------------------------------------//
    public GraphSummary(Graph graph, String workingDirectory) {
        this.targetsSummaryMap = new HashMap<>();
        this.firstRun = true;
        this.graphName = graph.getGraphName();
        this.workingDirectory = workingDirectory;
        this.totalPausedTime = Duration.ZERO;
        this.timePaused = null;

        TargetSummary currentTargetSummary;
        for(Target currentTarget : graph.getGraphTargets().values())
        {
            currentTargetSummary = new TargetSummary(currentTarget.getTargetName());

            this.targetsSummaryMap.put(currentTarget.getTargetName(), currentTargetSummary);
        }
    }

    //--------------------------------------------------Getters-----------------------------------------------------//
    public Boolean getFirstRun() {
        return this.firstRun;
    }

    public String getGraphName() { return this.graphName; }

    public Map<TargetSummary.ResultStatus, Integer> getAllResultStatus() { return this.allResultStatus; }

    public Duration getTime() { return this.totalTime; }

    public Map<String, TargetSummary> getTargetsSummaryMap() { return this.targetsSummaryMap; }

    public Integer getSkippedTargets() { return this.skippedTargets; }

    public String getWorkingDirectory() { return this.workingDirectory; }

    public void setSkippedTargetsToZero() { this.skippedTargets = 0; }

    public Set<String> getClosedSerialSets() { return this.closedSerialSets; }

    //--------------------------------------------------Setters-----------------------------------------------------//
    public void setFirstRun(Boolean firstRun) { this.firstRun = firstRun; }

    public void setRunningTargets(Target currentTarget, Boolean runningOrNot)
    {
        this.targetsSummaryMap.get(currentTarget.getTargetName()).setRunning(runningOrNot);

        for(Target requiredForTarget : currentTarget.getRequiredForTargets())
            setRunningTargets(requiredForTarget, runningOrNot);
    }

    public void setAllRequiredForTargetsOnSkipped(Target failedTarget, Target lastSkippedTarget)
    {
        TargetSummary newSkippedTargetSummary;
        for(Target newSkippedTarget : lastSkippedTarget.getRequiredForTargets())
        {
            newSkippedTargetSummary = this.targetsSummaryMap.get(newSkippedTarget.getTargetName());

            if(newSkippedTargetSummary.getRuntimeStatus().equals(TargetSummary.RuntimeStatus.Skipped))
                continue;

//            Platform.runLater(() -> System.out.println(newSkippedTarget.getTargetName() + " is skipped."));
            newSkippedTargetSummary.setRuntimeStatus(TargetSummary.RuntimeStatus.Skipped);
            newSkippedTargetSummary.setResultStatus(TargetSummary.ResultStatus.Failure);
            newSkippedTargetSummary.setSkipped(true);
            newSkippedTargetSummary.addNewSkippedByTarget(failedTarget.getTargetName());

            setAllRequiredForTargetsOnSkipped(failedTarget, newSkippedTarget);
        }
    }

    public synchronized Boolean isSkipped(String targetName)
    {
        return this.targetsSummaryMap.get(targetName).getRuntimeStatus().equals(TargetSummary.RuntimeStatus.Skipped);
    }

    //--------------------------------------------------Methods-----------------------------------------------------//
    public void startTheClock()
    {
        this.timeStarted = Instant.now();
    }

    public void stopTheClock()
    {
        Instant timeEnded = Instant.now();

        if(this.timePaused != null)
            continueTheClock();

        this.totalTime = Duration.between(this.timeStarted, timeEnded).minus(this.totalPausedTime);
        this.totalPausedTime = Duration.ZERO;
    }

    public void pauseTheClock()
    {
        this.timePaused = Instant.now();

        for(TargetSummary curr : this.targetsSummaryMap.values())
        {
            if(curr.getRuntimeStatus().equals(TargetSummary.RuntimeStatus.Waiting))
                curr.pausingWaitingTime();
        }
    }

    public void continueTheClock()
    {
        Instant timeEnded = Instant.now();

        this.totalPausedTime = Duration.between(this.timePaused, timeEnded).plus(this.totalPausedTime);
        this.timePaused = null;

        for(TargetSummary curr : this.targetsSummaryMap.values())
        {
            if(curr.getRuntimeStatus().equals(TargetSummary.RuntimeStatus.Waiting))
                curr.continuingWaitingTime();
        }
    }

    public void calculateResults()
    {
        this.allResultStatus = new HashMap<>();
        Integer succeeded = 0, failed = 0, warning = 0;

        for(TargetSummary current : this.targetsSummaryMap.values())
        {
            if(!current.isRunning())
                continue;

            if(current.isSkipped())
            {
                this.skippedTargets++;
                continue;
            }

            switch (current.getResultStatus())
            {
                case Failure:
                {
                    failed++;
                    break;
                }
                case Success:
                {
                    succeeded++;
                    break;
                }
                case Warning:
                {
                    warning++;
                    break;
                }
            }
        }

        this.allResultStatus.put(TargetSummary.ResultStatus.Success, succeeded);
        this.allResultStatus.put(TargetSummary.ResultStatus.Failure, failed);
        this.allResultStatus.put(TargetSummary.ResultStatus.Warning, warning);
    }

    public void MakeNewClosedSerialSets() {
        this.closedSerialSets = new HashSet<>();
    }

    public synchronized void UpdateTargetSummary(Target target, TargetSummary.ResultStatus resultStatus, TargetSummary.RuntimeStatus runtimeStatus, boolean init)
    {
        TargetSummary targetSummary = this.targetsSummaryMap.get(target.getTargetName());
        targetSummary.setResultStatus(resultStatus);
        targetSummary.setRuntimeStatus(runtimeStatus);

        if(runtimeStatus.equals(TargetSummary.RuntimeStatus.Waiting))
            targetSummary.startWaitingTime();

       else if(runtimeStatus.equals(TargetSummary.RuntimeStatus.InProcess))
            targetSummary.startProcessingTime();

       if(!init)
           removeClosedSerialSets(target);

        if(resultStatus.equals(TargetSummary.ResultStatus.Failure))
            setAllRequiredForTargetsOnSkipped(target, target);
    }

    public synchronized Boolean isTargetReadyToRun(Target target, Set<String> runningTargets)
    {
        if(this.targetsSummaryMap.get(target.getTargetName()).getRuntimeStatus().equals(TargetSummary.RuntimeStatus.Waiting))
            return true;

        for(String dependedTargetName : target.getAllDependsOnTargets())
        {
            //The depended target is not "in the game"
            if(!runningTargets.contains(dependedTargetName))
                continue;

            TargetSummary dependedTargetSummary = getTargetsSummaryMap().get(dependedTargetName);

            if(dependedTargetSummary.getRuntimeStatus().equals(TargetSummary.RuntimeStatus.Finished))
            {
                if(dependedTargetSummary.getResultStatus().equals(TargetSummary.ResultStatus.Failure))
                {
                    return false;
                }
                //The target finished with success / with warnings
                continue;
            }
            //The target is not finished its run
            return false;
        }

        //Runnable
        UpdateTargetSummary(target, TargetSummary.ResultStatus.Undefined, TargetSummary.RuntimeStatus.Waiting, false);
        return true;
    }
    public synchronized void addClosedSerialSets(Target target)
    {
        this.closedSerialSets.addAll(target.getSerialSets());
    }

    public synchronized void removeClosedSerialSets(Target target)
    {
        this.closedSerialSets.removeAll(target.getSerialSets());
    }

    public synchronized Boolean checkIfSerialSetsAreOpen(Set<String> otherSerialSet)
    {
        return Collections.disjoint(this.closedSerialSets, otherSerialSet);
    }

    public synchronized void resetSerialSets()
    {
        this.closedSerialSets.clear();
    }
}
