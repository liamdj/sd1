
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StrategiesTest {

    public static void main(String[] args) throws java.io.FileNotFoundException {

        final double[] ratios = { 0.05, 0.25, 1., 4., 20., };
        int numTrialsPer = 4;
        if (args.length > 0) {
            numTrialsPer = Integer.parseInt(args[0]);
        }
        final BufferedReader namesFile = new BufferedReader(new FileReader("students.txt"));
        final List<String> strategyNames = namesFile.lines().map(s -> String.format("Student_%s", s))
                .collect(Collectors.toList());
        final int N = strategyNames.size();
        assert N >= 10 : "Must have at least 10 strategies in students.txt!";

        final Admissions withStrategies = new Admissions(strategyNames);

        double[] results = new double[N];
        for (double S : ratios) {
            for (double T : ratios) {
                double[] res = withStrategies.oneEachTrials(numTrialsPer, new AdmissionsConfig(S, T, 1));
                for (int i = 0; i < N; i++) {
                    results[i] += res[i];
                }
            }
        }
        for (int i = 0; i < N; i++) {
            results[i] /= ratios.length * ratios.length;
        }
        double avgScore = 0;
        for (int i = 0; i < N; i++) {
            avgScore += results[i];
        }
        avgScore /= N;
        System.out.println("netID,mean score,fraction of total mean");
        String prevName = strategyNames.get(0).substring(8);
        double cumScore = results[0];
        int stratCount = 1;
        for (int i = 0; i != N; ++i) {
            String name = strategyNames.get(i).substring(8);
            if (!prevName.equals(name)) {
                System.out.println(prevName + "," + Double.toString(cumScore / stratCount) + ","
                        + Double.toString(cumScore / (avgScore * stratCount)));
                prevName = name;
                cumScore = 0;
                stratCount = 0;
            }
            cumScore += results[i];
            stratCount++;
        }
        System.out.println(prevName + "," + Double.toString(cumScore / stratCount) + ","
                + Double.toString(cumScore / (avgScore * stratCount)));
    }
}