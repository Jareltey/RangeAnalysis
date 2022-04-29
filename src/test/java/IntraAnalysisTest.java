package hw5;

import common.ErrorMessage;
import common.Utils;
import org.junit.Assert;
import org.junit.Test;
import soot.Main;
import soot.PackManager;
import soot.Transform;

public class IntraAnalysisTest extends AnalysisTest {
    void add_analysis() {
        analysisName = IntraAnalysisTransformer.ANALYSIS_NAME;
        PackManager.v().getPack("jap").add(
                new Transform(analysisName,
                        IntraAnalysisTransformer.getInstance())
        );
    }

    @Test
    public void testIntraAnalysis() {
        addTestClass("inputs.IntraTest");
        Main.main(getArgs());

        addExpected(ErrorMessage.POSSIBLE_EXCEED_ARRAY_LENGTH_WARNING, 16);
        addExpected(ErrorMessage.POSSIBLE_EXCEED_ARRAY_LENGTH_WARNING, 19);
        addExpected(ErrorMessage.EXCEED_ARRAY_LENGTH_ERROR, 31);
        addExpected(ErrorMessage.NEGATIVE_INDEX_ERROR, 33);
        addExpected(ErrorMessage.POSSIBLE_NEGATIVE_INDEX_WARNING, 57);
        addExpected(ErrorMessage.POSSIBLE_EXCEED_ARRAY_LENGTH_WARNING, 59);
        addExpected(ErrorMessage.EITHER_NEGATIVE_INDEX_OR_EXCEED_ARRAY_LENGTH_WARNING, 69);
        addExpected(ErrorMessage.POSSIBLE_EXCEED_ARRAY_LENGTH_WARNING, 83);
        addExpected(ErrorMessage.NEGATIVE_INDEX_ERROR, 94);

        Assert.assertEquals(expected, Utils.getErrors());
    }
}
