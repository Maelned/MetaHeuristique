package jobshop.solvers;


import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;

public class GloutonSolver implements Solver {

    String priority = null;
    public void setPriority(String prio) {
        priority = prio;
    }
    public String getPriority(){return priority;}


    @Override
    public Result solve(Instance instance, long deadline) {
        ResourceOrder rso = new ResourceOrder(instance);
        ArrayList<Task> Task_feasible = new ArrayList<>();


        int[] startTimes = new int[instance.numJobs];
        int[] timeToEnd = new int[instance.numJobs];
        int[] machineTime = new int[instance.numMachines];
        Arrays.fill(startTimes,0);
        Arrays.fill(machineTime,0);

        for (int i = 0; i < instance.numJobs; i++) {
            timeToEnd[i] = 0;
            for (int j = 0; j < instance.numTasks; j++) {
                timeToEnd[i] += instance.duration(i, j);
            }
        }

        //Init
        for(int job = 0; job < instance.numJobs; job++){
            Task task = new Task(job,0);
            Task_feasible.add(task);
        }

        //Loop

        while(!Task_feasible.isEmpty()) {
            int duration_tmp = 0;
            int duration_current = 0;
            int duration;
            int dateDebut;
            int best_dateDebut = -1;
            ArrayList<Task> Array_Task_tmp = new ArrayList<>();
            Task Task_tmp = null;

            for (Task current : Task_feasible) {


                int currentJob = current.job;
                if (Task_tmp == null) {
                    Task_tmp = current;
                } else {


                    switch (priority) {

                        case "SPT":
                            if (instance.duration(Task_tmp) >= instance.duration(current)) {
                                Task_tmp = current;
                            }
                            break;
                        case "LRPT":
                            for (int y = 0; y < instance.numTasks; y++) {
                                duration_tmp += instance.duration(Task_tmp.job, y);
                            }
                            for (int y = 0; y < instance.numTasks; y++) {
                                duration_current += instance.duration(current.job, y);

                            }
                            if (duration_tmp <= duration_current) {
                                Task_tmp = current;
                            }
                            break;
                        case "LPT":
                            if (instance.duration(Task_tmp) <= instance.duration(current)) {
                                Task_tmp = current;
                            }
                            break;
                        case "SRPT":
                            for (int y = 0; y < instance.numTasks; y++) {
                                duration_tmp += instance.duration(Task_tmp.job, y);
                            }
                            for (int y = 0; y < instance.numTasks; y++) {
                                duration_current += instance.duration(current.job, y);

                            }
                            if (duration_tmp >= duration_current) {
                                Task_tmp = current;
                            }
                            break;

                        case "EST_SPT":
                            currentJob = current.job;
                            Task_tmp = null;

                            if (best_dateDebut == -1) {
                                best_dateDebut = Math.max(startTimes[currentJob], machineTime[instance.machine(current)]);
                                Array_Task_tmp.add(current);

                            } else {
                                dateDebut = Math.max(startTimes[currentJob], machineTime[instance.machine(current)]);
                                if (dateDebut == best_dateDebut) {
                                    Array_Task_tmp.add(current);
                                }
                                if (dateDebut < best_dateDebut) {
                                    Array_Task_tmp.clear();
                                    Array_Task_tmp.add(current);
                                    best_dateDebut = dateDebut;
                                }
                            }


                            int shortest_Duration = -1;

                            for (Task current_tmp : Array_Task_tmp) {

                                if (shortest_Duration == -1) {
                                    shortest_Duration = instance.duration(current_tmp);
                                    Task_tmp = current_tmp;
                                } else {
                                    duration = instance.duration(current_tmp);
                                    if (duration < shortest_Duration) {
                                        Task_tmp = current_tmp;
                                        shortest_Duration = duration;
                                    }
                                }

                            }

                            break;
                        case "EST_LRPT":
                            duration = 0;
                            Task Task2_tmp = null;

                            if (best_dateDebut == -1) {
                                best_dateDebut = Math.max(startTimes[currentJob], machineTime[instance.machine(current)]);
                                Array_Task_tmp.add(current);

                            } else {
                                dateDebut = Math.max(startTimes[currentJob], machineTime[instance.machine(current)]);
                                if (dateDebut == best_dateDebut) {
                                    Array_Task_tmp.add(current);
                                }
                                if (dateDebut < best_dateDebut) {
                                    Array_Task_tmp.clear();
                                    Array_Task_tmp.add(current);
                                    best_dateDebut = dateDebut;
                                }
                            }


                            for (Task task : Array_Task_tmp) {
                                int currentJob_tmp = task.job;
                                if (Task2_tmp == null) {
                                    Task2_tmp = task;
                                    duration = timeToEnd[currentJob_tmp];
                                } else if (duration < timeToEnd[currentJob_tmp]) {
                                    Task2_tmp = task;
                                    duration = timeToEnd[currentJob_tmp];
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }



            int rsc = instance.machine(Task_tmp);
            int job = Task_tmp.job;
            int task = Task_tmp.task;
            rso.addTask(rsc, Task_tmp);
            Task_feasible.remove(Task_tmp);


            if (instance.numTasks-1 > Task_tmp.task) {
                Task_feasible.add(new Task(job, task + 1));
            }
        }
        return new Result(instance, rso.toSchedule(),Result.ExitCause.Blocked);
    }
}



