package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;


import java.util.*;

public class DescentSolver implements Solver {

    //Creating a GloutonSolver solution.
    GloutonSolver solverGlouton = new GloutonSolver();

    /**
     * A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     * <p>
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     * <p>
     * The block with : machine = 1, Task_F= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     */
    static class Block {
        /**
         * machine on which the block is identified
         */
        final int machine;
        /**
         * index of the first task of the block
         */
        final int Task_F;
        /**
         * index of the last task of the block
         */
        final int lastTask;

        Block(int machine, int Task_F, int lastTask) {
            this.machine = machine;
            this.Task_F = Task_F;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     * <p>
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     * <p>
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /**
         * Apply this swap on the given resource order, transforming it into a new solution.
         */
        public void applyOn(ResourceOrder order) {
            int swapTask1 = this.t1;
            int swapTask2 = this.t2;
            int machine = this.machine;
            Task tmp = order.tasksByMachine[machine][swapTask1];
            order.tasksByMachine[machine][swapTask1] = order.tasksByMachine[machine][swapTask2];
            order.tasksByMachine[machine][swapTask2] = tmp;

        }
    }
    public void setGloutonPriority(String prio){
        solverGlouton.setPriority(prio);
    }
    @Override
    public Result solve(Instance instance, long deadline) {
        if(solverGlouton.getPriority() == null){
            solverGlouton.setPriority("SPT");
        }
        //Init
        Result result = solverGlouton.solve(instance, deadline);

        ResourceOrder rso = new ResourceOrder(result.schedule);
        //Memorize the best solution
        ResourceOrder rso_optimal = rso.copy();
        int makespan_optimal = rso_optimal.toSchedule().makespan();

        boolean new_Voisin = true;
        //Stop if no better neighboor exists.
        while (new_Voisin) {
            new_Voisin=false;
            ResourceOrder rso_tmp= rso_optimal.copy();
            List<Swap> ListSwap = new ArrayList<>();
            List<Block> BlockList = this.blocksOfCriticalPath(rso_tmp);

            for (Block block : BlockList) {
                ListSwap.addAll(this.neighbors(block));
            }
            //Selecting the best neighboor on the ListSwap
            for (Swap swap : ListSwap) {
                ResourceOrder current = rso_tmp.copy();
                swap.applyOn(current);
                Schedule currentSchedule = current.toSchedule();

                if (currentSchedule != null) {
                    int makespan_cur = currentSchedule.makespan();

                    if (makespan_optimal > makespan_cur) {
                        rso_optimal = current;
                        makespan_optimal = makespan_cur;
                        new_Voisin = true;
                    }
                }
            }
        }
        return new Result(instance, rso_optimal.toSchedule(), Result.ExitCause.Blocked);
    }
    /**
     * Method created in order to make other methods easier to read
     * Returns the index of a certain task on a machine
     */
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
    List<Block> blocksOfCriticalPath(ResourceOrder order) {

        Schedule schedule = order.toSchedule(); //Creating the Schedule from the rso Order
        List<Task> listTask = schedule.criticalPath(); // Getting the criticalPath from the schedule
        int Index_S = -1;
        int Index_E;
        int currentMachine = -1;
        ArrayList<Block> BlockList = new ArrayList<>(); //List of block returned by this method
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
                    BlockList.add(new Block(machine, Index_S, Index_E));
                }
            } else if (j == listTask.size() - 2) {
                Index_E = nextIndexTask;
                if (Index_S != Index_E) {
                    BlockList.add(new Block(machine, Index_S, Index_E));
                } }

        }
        return BlockList;
    }

    /**
     * For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood
     */
    List<Swap> neighbors(Block block) {
        int Task_F = block.Task_F;
        int Task_L = block.lastTask;
        int machine = block.machine;
        ArrayList<Swap> SwapList = new ArrayList<>();
        //Testing if Last and first tasks are already neighbors
        if((Task_L-Task_F) == 1){
            //If so : creating 1 swap
            SwapList.add(new Swap(machine,Task_L,Task_F));
        }else{
            //If not : creating 2 swap, for the task and their neigbors
            SwapList.add(new Swap(machine,Task_F,Task_F+1));
            SwapList.add(new Swap(machine,(Task_L-1),Task_L));
        }
        return SwapList;
    }
}