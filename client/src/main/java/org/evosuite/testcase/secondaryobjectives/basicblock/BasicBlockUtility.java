package org.evosuite.testcase.secondaryobjectives.basicblock;

import org.evosuite.TestGenerationContext;
import org.evosuite.ga.Chromosome;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BasicBlock;
import org.evosuite.testcase.TestChromosome;

import java.util.*;
import java.util.stream.Collectors;

public class BasicBlockUtility {

    protected HashMap<Chromosome,Set<BasicBlock>> fullyCoveredBlocks= new HashMap<>();
    protected HashMap<Chromosome,Set<BasicBlock>> semiCoveredBlocks= new HashMap<>();
    BasicBlock targetBlock;
    /*
     Checks if the given CoveredBasicBlocks has the same coverage (returns true) or not.
    */
    public boolean sameBasicBlockCoverage(TestChromosome chromosome1, TestChromosome chromosome2) {
        Collection<BasicBlock> fullyCovered1 = getFullyCoveredBasicBlocks(chromosome1);
        Collection<BasicBlock> fullyCovered2 = getFullyCoveredBasicBlocks(chromosome2);
        if(!fullyCovered1.equals(fullyCovered2)){
            return false;
        }

        Collection<BasicBlock> semiCovered1 = getSemiCoveredBasicBlocks(chromosome1);
        Collection<BasicBlock> semiCovered2 = getSemiCoveredBasicBlocks(chromosome2);
        return semiCovered1.equals(semiCovered2);
    }
    /*
         Returns the fully-covered basic blocks
     */
    private Set<BasicBlock> getFullyCoveredBasicBlocks(Chromosome chromosome1) {
        if(fullyCoveredBlocks.containsKey(chromosome1)){
            return fullyCoveredBlocks.get(chromosome1);
        }

        return new HashSet<>();

    }

    /*
        Returns the semi-covered basic blocks
    */
    public List<BasicBlock> getSemiCoveredBasicBlocks(Chromosome chromosome1) {
        if(semiCoveredBlocks.containsKey(chromosome1)){
            return semiCoveredBlocks.get(chromosome1).stream().collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /*
    Search the given actual control flow graph to find the closest basic block to the given line number.
    */
    private BasicBlock findTheClosestBlock(List<BasicBlock> semiCoveredBasicBlocks, int targetLine){
        String targetClass = semiCoveredBasicBlocks.get(0).getClassName();
        String targetMethod = semiCoveredBasicBlocks.get(0).getMethodName();

        // Find the control flow graph of target method
        ActualControlFlowGraph targetMethodCFG = getTargetMethodCFG(targetClass,targetMethod);

        // Find a basic block which contains the target line
        BasicBlock targetBlock = findTargetBlock(targetMethodCFG,targetLine);

        BasicBlock result=null;
        int minimumDistance = Integer.MAX_VALUE;

        for (BasicBlock currentBlock: semiCoveredBasicBlocks){
            int distance = targetMethodCFG.getDistance(currentBlock,targetBlock);
            if (distance <= minimumDistance){
                result = currentBlock;
                minimumDistance = distance;
            }
        }
        if (result == null){
            throw new IllegalStateException("The selected basic block is null");
        }
        return result;
    }

    /*
        Search the given actual control flow graph to find a basic block, which contains the given line number.
     */
    private BasicBlock findTargetBlock(ActualControlFlowGraph targetMethodCFG, int targetLine) {
        if (targetBlock != null){
            return targetBlock;
        }
        // check the basic blocks in the targetMethodCFG iteratively
        List<BasicBlock> visitedBasicBlocks = new ArrayList<>();
        List<BasicBlock> BasicBlocksToVisit = new LinkedList<>();
        // Start with the entry point node
        BasicBlock entryBasicBlock = targetMethodCFG.getEntryPoint().getBasicBlock();
        BasicBlocksToVisit.add(entryBasicBlock);

        while (BasicBlocksToVisit.size() > 0) {
            // Get a basic block
            BasicBlock currentBasicBlock = BasicBlocksToVisit.remove(0);
            visitedBasicBlocks.add(currentBasicBlock);
            int firstLine = currentBasicBlock.getFirstLine();
            int lastLine = currentBasicBlock.getLastLine();
            if (targetLine >= firstLine && targetLine<= lastLine){
                targetBlock = currentBasicBlock;
                return currentBasicBlock;
            }
            for (BasicBlock child: targetMethodCFG.getChildren(currentBasicBlock)){
                if (!visitedBasicBlocks.contains(child)){
                    BasicBlocksToVisit.add(child);
                }
            }
        }
        throw new IllegalArgumentException("The target line is not available in the given control flow graph!");
    }


    /*
        Returns the actual control flow graph of the requested method
     */
    private ActualControlFlowGraph getTargetMethodCFG(String targetClass, String targetMethod){
        GraphPool graphPool = GraphPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT());
        return graphPool.getActualCFG(targetClass,targetMethod);
    }

    /*
        Returns the lines that their coverage is important for us in the current comparison
     */
    private  Set<Integer> detectInterestingCoveredLines(TestChromosome chromosome, BasicBlock targetBlock, int targetLine) {
        Set<Integer> result = new HashSet<>();
        int lastLine = Integer.min(targetLine,targetBlock.getLastLine());

        Set<Integer> coveredLines  = chromosome.getLastExecutionResult().getTrace().getCoveredLines(targetBlock.getClassName());

        for (Integer line: coveredLines){
            if (line >= targetBlock.getFirstLine() && line <= lastLine){
                result.add(line);
            }
        }

        return result;
    }

    /*
    Checks if the fully covered blocks in the first parameter (chromosome1) is a subset of fully covered blocks in the second parameter (chromosome2)
     */
    public  boolean isSubset(TestChromosome chromosome1, TestChromosome chromosome2) {

        Collection<BasicBlock> fullyCovered1 = getFullyCoveredBasicBlocks(chromosome1);
        Collection<BasicBlock> fullyCovered2 = getFullyCoveredBasicBlocks(chromosome2);
        if(!fullyCovered2.containsAll(fullyCovered1)){
            return false;
        }

        Collection<BasicBlock> semiCovered1 = getSemiCoveredBasicBlocks(chromosome1);
        Collection<BasicBlock> semiCovered2 = getSemiCoveredBasicBlocks(chromosome2);
        return semiCovered2.containsAll(semiCovered1);
    }

    /*
    Returns the coverage size. The fully covered blocks are counted two times.
     */
    public  int getCoverageSize(Chromosome chromosome) {
        Collection<BasicBlock> fullyCovered = getFullyCoveredBasicBlocks(chromosome);
        Collection<BasicBlock> semiCovered = getSemiCoveredBasicBlocks(chromosome);

        return fullyCovered.size()*2+ semiCovered.size();
    }

    /*
        Checks if the given chromosome has covered all of the given basic block
     */
    private  boolean isFullyCovered(BasicBlock currentBasicBlock, Set<Integer> coveredLines, int targetLine) {
        if(currentBasicBlock.isEntryBlock()){
            return true;
        }
        int lastLineNumber = currentBasicBlock.getLastLine();
        if (targetLine < lastLineNumber){
            lastLineNumber = targetLine;
        }

        return coveredLines.contains(lastLineNumber);
    }

    /*
        Checks if the given chromosome has reached to the given basic block
     */
    private  boolean isTouched(BasicBlock currentBasicBlock, Set<Integer> coveredLines) {
        if(currentBasicBlock.isEntryBlock()){
            return true;
        }
        int firstLineNumber = currentBasicBlock.getFirstLine();

        return coveredLines.contains(firstLineNumber);
    }

    /*
        Returns all of the basic blocks in the target method, which are covered (either fully or semi) by the given chromosome.
     */
    // toDo: Should we filter out the irrelevant basic blocks?
    public  Collection<CoveredBasicBlock> collectCoveredBasicBlocks(TestChromosome chromosome, String targetClass, String targetMethod, int targetLine) {
        Set<CoveredBasicBlock> coveredBasicBlocks = new HashSet<>();

        // Find the control flow graph of target method
        ActualControlFlowGraph targetMethodCFG = getTargetMethodCFG(targetClass,targetMethod);

        // find the covered lines in the target method by the given chromosome
        Set<Integer> coveredLines  = chromosome.getLastExecutionResult().getTrace().getCoveredLines(targetClass);

        // check the basic blocks in the targetMethodCFG iteratively
        List<BasicBlock> visitedBasicBlocks = new ArrayList<>();
        List<BasicBlock> BasicBlocksToVisit = new LinkedList<>();
        // Start with the entry point node
        BasicBlock entryBasicBlock = targetMethodCFG.getEntryPoint().getBasicBlock();



        BasicBlocksToVisit.add(entryBasicBlock);

        while (BasicBlocksToVisit.size() > 0){
            // Get a basic block
            BasicBlock currentBasicBlock = BasicBlocksToVisit.remove(0);
            visitedBasicBlocks.add(currentBasicBlock);
            // check if it is covered
            if(isTouched(currentBasicBlock,coveredLines)){
                // if it is covered, first, we add it to the final list.
                if(isFullyCovered(currentBasicBlock,coveredLines,targetLine)){
                    coveredBasicBlocks.add(new CoveredBasicBlock(currentBasicBlock,true));
                    if(!fullyCoveredBlocks.containsKey(chromosome)){
                        fullyCoveredBlocks.put(chromosome,new HashSet<>());
                    }
                    fullyCoveredBlocks.get(chromosome).add(currentBasicBlock);
                }else{
                    coveredBasicBlocks.add(new CoveredBasicBlock(currentBasicBlock,false));
                    if(!semiCoveredBlocks.containsKey(chromosome)){
                        semiCoveredBlocks.put(chromosome,new HashSet<>());
                    }
                    semiCoveredBlocks.get(chromosome).add(currentBasicBlock);
                }

                // Second we check its children to check them too.
                for (BasicBlock child: targetMethodCFG.getChildren(currentBasicBlock)){
                    if(!visitedBasicBlocks.contains(child)){
                        BasicBlocksToVisit.add(child);
                    }
                }
            }
        }
        return coveredBasicBlocks;
    }



    /*
    Check the line coverage in the semi covered blocks. More line coverage is better
     */

    public  int compareCoveredLines(TestChromosome chromosome1, TestChromosome chromosome2, List<BasicBlock> semiCoveredBasicBlocks, int targetLine) {

        // First, we check if we have any semiCoveredBlock
        if (semiCoveredBasicBlocks.isEmpty()){
            return 0;
        }

        // Then, we remove blocks, in which the target block is not accessible
        String targetClass = semiCoveredBasicBlocks.get(0).getClassName();
        String targetMethod = semiCoveredBasicBlocks.get(0).getMethodName();
        ActualControlFlowGraph targetMethodCFG = getTargetMethodCFG(targetClass,targetMethod);
        BasicBlock targetBlock = findTargetBlock(targetMethodCFG,targetLine);
        Iterator<BasicBlock> iter = semiCoveredBasicBlocks.iterator();
        while (iter.hasNext()){
            BasicBlock scBlock = iter.next();
            if (targetMethodCFG.getDistance(scBlock,targetBlock)<0){
                iter.remove();
            }
        }

        // We continue if we still have any candidate for line comparison
        if (semiCoveredBasicBlocks.isEmpty()){
            return 0;
        }

        // Next, we select the target block
        BasicBlock targetSemiCoveredBlock;
        if (semiCoveredBasicBlocks.size() == 1){
            // if we only have one semiCoveredBlock, we will select it  as our target block
            targetSemiCoveredBlock = semiCoveredBasicBlocks.get(0);
        }else{
            // if we have more than one block, we select the closest one to the target line
            targetSemiCoveredBlock = findTheClosestBlock(semiCoveredBasicBlocks, targetLine);
        }



        // find the covered lines in the target method by the given chromosome
        Collection<Integer> coveredLines1  = detectInterestingCoveredLines(chromosome1,targetSemiCoveredBlock,targetLine);
        Collection<Integer> coveredLines2  = detectInterestingCoveredLines(chromosome2,targetSemiCoveredBlock,targetLine);

        if (coveredLines1.equals(coveredLines2)){
            // Same coverage
            return 0;
        }

        // the returned value is >0 if the number of covered lines by chromosome2 is more than chromosome1 and vice versa
        return coveredLines2.size() - coveredLines1.size();
    }



    public void clear() {
        fullyCoveredBlocks.clear();
        semiCoveredBlocks.clear();
        targetBlock=null;
    }
}
