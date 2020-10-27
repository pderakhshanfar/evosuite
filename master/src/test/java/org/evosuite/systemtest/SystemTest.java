package org.evosuite.systemtest;

import com.examples.with.different.packagename.implicitbranch.ExternalImplicitException;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.junit.Test;

public class SystemTest {

    @Test
    public void simpleTest(){
//        Properties.SECONDARY_OBJECTIVE = new Properties.SecondaryObjective[] {Properties.SecondaryObjective.BasicBlockCoverage};
        EvoSuite evosuite = new EvoSuite();


        String targetClass = ExternalImplicitException.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;


        String[] command = new String[] { "-class", targetClass ,
                "-Dsecondary_objectives="+Properties.SecondaryObjective.BBCOVERAGE.name()+":"+Properties.SecondaryObjective.TOTAL_LENGTH,
                "-Doutput_variables=TARGET_CLASS,search_budget,Total_Time,Length,Size,LineCoverage,BranchCoverage,OutputCoverage,WeakMutationScore,Implicit_MethodExceptions,MutationScore",
                "-Dpopulation="+70,
                "-Dreport_dir="+"/Users/pooria/IdeaProjects/evosuite/master/src/test/java/org/evosuite/systemtest"
        };

        Object result = evosuite.parseCommandLine(command);
    }
}
