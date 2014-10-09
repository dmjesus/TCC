package analysis;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;

public class CoverageAnalysis {
	
	private static final Format DECIMAL_FORMAT = new DecimalFormat("0.00"); 

	public static String getCoverage(String className) {
		className = className.replace('.', '/') + ".class";
		String ret = "";
		ExecutionDataStore executionData = new ExecutionDataStore();
		SessionInfoStore sessionInfos = new SessionInfoStore();
		FileLoader.getData().collect(executionData, sessionInfos, false);
		FileLoader.getRuntime().shutdown();

		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

		try {
			analyzer.analyzeClass(FileLoader.getClassLoader()
					.getResourceAsStream(className));
		} catch (IOException e) {
			e.printStackTrace();
			return ret;
		}
		for (final IClassCoverage cc : coverageBuilder.getClasses()) {
			ret += "Coverage of class " + cc.getName() + ":\n";
			ret += printCounter("instructions", cc.getInstructionCounter());
			ret += printCounter("branches", cc.getBranchCounter());
			ret += printCounter("lines", cc.getLineCounter());
			ret += printCounter("methods", cc.getMethodCounter());
			ret += printCounter("complexity", cc.getComplexityCounter());
		}
		return ret;
	}

	private static String printCounter(final String unit, final ICounter counter) {
		double covered = Double.valueOf(counter.getCoveredRatio()) * 100.0;
		covered = Double.isNaN(covered) ? 0 : covered;
		Integer total = Integer.valueOf(counter.getTotalCount());
		return DECIMAL_FORMAT.format(covered) + "% of " + total + " " + unit + ".\n"; 
	}

}
