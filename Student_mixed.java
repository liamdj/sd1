// create by Liam Johansson

import java.util.Arrays;
import java.util.List;

public class Student_mixed implements Student {

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

    public int[] getApplications(int N, double S, double T, double W, double aptitude, List<Double> schools,
            List<Double> synergies) {

        // Finds schools to apply to
        School[] preferences = new School[N];
        for (int i = 0; i < N; i++) {
            preferences[i] = new School(i, (aptitude + synergies.get(i)) / (S + W) * schools.get(i) + synergies.get(i));
        }
        Arrays.sort(preferences);

        // Sort schools applying to by actual preference
        School[] willApply = new School[10];
        for (int i = 0; i < 10; i++) {
            int uni = preferences[i].index;
            willApply[i] = new School(uni, schools.get(uni) + synergies.get(uni));
        }
        Arrays.sort(willApply);

        int[] ret = new int[10];
        for (int i = 0; i < 10; i++) {
            ret[i] = willApply[i].index;
        }
        return ret;
    }

}