// created by Liam Johansson

import java.util.Arrays;
import java.util.List;

public class Student_percentile implements Student {

    // from Student_holist
    private class School implements Comparable<School> {
        public School(int i, double q) {
            index = i;
            quality = q;
        }

        private int index;
        private double quality;

        public int compareTo(School n) { // smaller pairs are higher quality
            int ret = Double.compare(n.quality, quality);
            return (ret == 0) ? (Integer.compare(index, n.index)) : ret;
        }
    }

    // Computes CDF of sum of 2 independent uniform distributions
    // drawn from [0, A] and [0, B]
    private double percentile(double x, double A, double B) {
        double a = Math.min(A, B);
        double b = Math.max(A, B);
        if (x < a)
            return Math.pow(x, 2) / (2 * a * b);
        else if (x < b)
            return (2 * x - a) / (2 * b);
        else
            return 1 - Math.pow(a + b - x, 2) / (2 * a * b);
    }

    //
    public int[] getApplications(int N, double S, double T, double W, double aptitude, List<Double> schools,
            List<Double> synergies) {
        // Finds schools to apply to
        School[] applications = new School[schools.size()];
        for (int i = 0; i < applications.length; i++) {
            // fraction of students I am a better applicant than for this school
            double myPercentile = percentile(aptitude + synergies.get(i), S, W);
            // fraction of students I need to beat to have a good chance
            // of getting into this school
            // weighted sum of school's quality rank and EV of max synergy among students
            double goalPercentile;
            if (T != 0)
                goalPercentile = (S * schools.get(i) / T + W * (N - 1) / N) / (S + W);
            else
                goalPercentile = (N - 1) / N;

            double weight = Math.min(1, myPercentile / goalPercentile);
            applications[i] = new School(i, Math.pow(weight, 0.5) * (weight * schools.get(i) + synergies.get(i)));
        }
        Arrays.sort(applications);

        // Sort schools applying to by actual preference
        School[] preferences = new School[10];
        for (int i = 0; i < preferences.length; i++) {
            int uni = applications[i].index;
            preferences[i] = new School(uni, schools.get(uni) + synergies.get(uni));
        }
        Arrays.sort(preferences);

        int[] ret = new int[preferences.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = preferences[i].index;
        }
        return ret;
    }
}