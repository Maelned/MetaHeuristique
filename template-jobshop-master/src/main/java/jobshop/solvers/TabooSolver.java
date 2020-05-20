package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.*;

public class TabooSolver implements Solver{

    GloutonSolver gloutonSolver = new GloutonSolver();

    public void setGloutonPrio(String prio){
        gloutonSolver.setPriority(prio);
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        gloutonSolver.setPriority("EST_LRPT");
        Result r = gloutonSolver.solve(instance, deadline);

        ResourceOrder rso = new ResourceOrder(r.schedule);
        ResourceOrder currentSolution = rso.copy();

        int nbJobs = instance.numJobs;
        int nbMachines = instance.numMachines;
        int size = nbJobs * nbMachines;

        int[][] sTaboo = new int[size][size];

        //Init
        for(int i = 0 ; i < size ; i ++){
            for(int j = 0 ; j < size ; j ++){
                sTaboo[i][j] = 0;
            }
        }
        
        //loop
        int k = 1;
        int maxIter = 200;
        while(k < maxIter) {

            List<DescentSolver.Swap> ListSwap = new ArrayList<>();
            List<DescentSolver.Block> ListBlock = this.blocksOfCriticalPath(currentSolution);
            for (DescentSolver.Block block : ListBlock) {
                ListSwap.addAll(this.neighbors(block));
            }
            // A finir
            k++;
        }
        return new Result(null, null, null);
    }
    //Copy of the descent Solver's methods
    int indexTaskOnMachine(ResourceOrder rso, int machine, Task taskObj) {
        int index_T = -1;
        for (int i = 0; i < rso.instance.numJobs; i++) {
            if (rso.tasksByMachine[machine][i].equals(taskObj)) {
                index_T = i;
            }
        }
        return index_T;
    }

    /**
     * Returns a list of all blocks of the critical path.
     */
    List<DescentSolver.Block> blocksOfCriticalPath(ResourceOrder order) {

        Schedule schedule = order.toSchedule(); //Creating the Schedule from the rso Order
        List<Task> listTask = schedule.criticalPath(); // Getting the criticalPath from the schedule
        int Index_S = -1;
        int Index_E;
        int currentMachine = -1;
        ArrayList<DescentSolver.Block> BlockList = new ArrayList<>(); //List of block returned by this method
        for (int j = 0; j < listTask.size() - 1; j++) {
            int task = listTask.get(j).task;
            int job = listTask.get(j).job;
            int machine = order.instance.machine(job, task);
            //Here we call the method created just above
            int index_T = indexTaskOnMachine(order, machine, listTask.get(j));
            //Testing for the 1st iteration and to set currentMachine
            if (currentMachine == -1 || currentMachine != machine) {
                currentMachine = machine;
                Index_S = index_T;
            }
            //Getting the next Task/Job/Machine/index_T for the path
            int nextTask = listTask.get(j + 1).task;
            int nextJob = listTask.get(j + 1).job;
            int nextMachine = order.instance.machine(nextJob, nextTask);
            int nextIndexTask = indexTaskOnMachine(order, nextMachine, listTask.get(j + 1));

            if (nextMachine != machine) {
                Index_E = index_T;
                if (Index_S != Index_E) {
                    BlockList.add(new DescentSolver.Block(machine, Index_S, Index_E));
                }
            } else if (j == listTask.size() - 2) {
                Index_E = nextIndexTask;
                if (Index_S != Index_E) {
                    BlockList.add(new DescentSolver.Block(machine, Index_S, Index_E));
                } }

        }
        return BlockList;
    }

    /**
     * For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood
     */
    List<DescentSolver.Swap> neighbors(DescentSolver.Block block) {
        int Task_F = block.Task_F;
        int Task_L = block.lastTask;
        int machine = block.machine;
        ArrayList<DescentSolver.Swap> SwapList = new ArrayList<>();
        //Testing if Last and first tasks are already neighbors
        if((Task_L-Task_F) == 1){
            //If so : creating 1 swap
            SwapList.add(new DescentSolver.Swap(machine,Task_L,Task_F));
        }else{
            //If not : creating 2 swap, for the task and their neigbors
            SwapList.add(new DescentSolver.Swap(machine,Task_F,Task_F+1));
            SwapList.add(new DescentSolver.Swap(machine,(Task_L-1),Task_L));
        }
        return SwapList;
    }
}