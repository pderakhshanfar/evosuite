package org.evosuite.testcase.secondaryobjectives.basicblock;

import org.evosuite.Properties;

import org.evosuite.coverage.mutation.WeakMutationTestFitness;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;


public class BasicBlockCoverage extends SecondaryObjective<TestChromosome> {

    BasicBlockUtility basicBlockUtility;
    public BasicBlockCoverage(){
        super();
        basicBlockUtility = new BasicBlockUtility();
    }

    public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2, FitnessFunction objective){
        // Get target class and method
        String targetClass = Properties.TARGET_CLASS;
        String targetMethod = this.getTargetMethod(objective);

        /* If target method is zero, it means that the objective does not use approach level and branch distance.
         * So, we dont need to use BBC here.
        */
        if(targetMethod == null){
            return 0;
        }

        int targetLine = this.getTargetLineNumber(objective);

        // BBC is applicable on weak mutation if and only if the statement is not covered (AL + BD != 0).
        // Here, we check the coverage of the target line for weak mutation objectives
        if(objective instanceof WeakMutationTestFitness &&
                chromosome1.getLastExecutionResult().getTrace().getCoveredLines(targetClass).contains(targetLine)){
            return 0;
        }


        int finalValue;

        // collect fully/semi covered basic blocks
        basicBlockUtility.collectCoveredBasicBlocks(chromosome1,targetClass,targetMethod,targetLine);
        basicBlockUtility.collectCoveredBasicBlocks(chromosome2,targetClass,targetMethod,targetLine);

        // First, we check if chromosomes have the same fully/semi covered basic blocks.
        if(basicBlockUtility.sameBasicBlockCoverage(chromosome1,chromosome2)){
            // if it is the case, we go deeper and check their line coverage in the closest semi-covered block to the target statement.
            finalValue=basicBlockUtility.compareCoveredLines(chromosome1,chromosome2,basicBlockUtility.getSemiCoveredBasicBlocks(chromosome1),targetLine);
        }else if(basicBlockUtility.isSubset(chromosome2,chromosome1) || basicBlockUtility.isSubset(chromosome1,chromosome2)){
            // chromosome 2 coverage is a subset of chromosome 1 coverage
            // or
            // chromosome 1 coverage is a subset of chromosome 2 coverage
            // the returned value is >0 if chromosome1 is a subset of chromosome2 and vice versa.
            finalValue = basicBlockUtility.getCoverageSize(chromosome2) - basicBlockUtility.getCoverageSize(chromosome1);
        }else {
            // Here, we cannot say which test is better. So, we set the final value to zero.
            finalValue= 0;
        }
        // clear the utility
        basicBlockUtility.clear();

        return finalValue;
    }

    private String getTargetMethod(FitnessFunction objective) {
        if(objective instanceof org.evosuite.coverage.line.LineCoverageTestFitness ){
            return ((org.evosuite.coverage.line.LineCoverageTestFitness) objective).getTargetMethod();
        }else if(objective instanceof  org.evosuite.coverage.branch.BranchCoverageTestFitness){
            return ((org.evosuite.coverage.branch.BranchCoverageTestFitness) objective).getTargetMethod();
        }else if(objective instanceof org.evosuite.coverage.mutation.WeakMutationTestFitness){
            return ((org.evosuite.coverage.mutation.WeakMutationTestFitness) objective).getTargetMethod();
        }
        return null;
    }

    private int getTargetLineNumber(FitnessFunction objective){
        if(objective instanceof org.evosuite.coverage.line.LineCoverageTestFitness ){
            return ((org.evosuite.coverage.line.LineCoverageTestFitness) objective).getLine();
        }else if(objective instanceof  org.evosuite.coverage.branch.BranchCoverageTestFitness){
            return ((org.evosuite.coverage.branch.BranchCoverageTestFitness) objective).getBranchGoal().getLineNumber();
        }else if(objective instanceof org.evosuite.coverage.mutation.WeakMutationTestFitness){
            return ((org.evosuite.coverage.mutation.WeakMutationTestFitness) objective).getMutation().getLineNumber();
        }
        return -1;
    }
    /*
    * This method is only called in places that BBC is irrelevant.
    * For instance, in archive, since archive is about the covered targets and we dont have any target to reach, we call this method to get 0 all of the times.
     * */
    @Override
    public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2) {
        return 0;
    }

    @Override
    public int compareGenerations(TestChromosome parent1, TestChromosome parent2,
                                  TestChromosome child1, TestChromosome child2) {
        return 0;
    }
}
