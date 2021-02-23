// Student_lk2percentile.java:
// Created by Liam Johansson and Ken Oku

import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Student_lk2percentile implements Student {

    private static final int TRIALS = 200;
    private static final int OPPONENT_APPS = 10;
    private static final double MIN_SW = 3;
    private static final double MIN_TW = 3;

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

    // from Admissions
    private static class StudentPair implements Comparable<StudentPair> {
        public StudentPair(int i, double q) {
            index = i;
            quality = q;
        }

        public int getIndex() {
            return index;
        }

        private int index;
        private double quality;

        public int compareTo(StudentPair n) { // sort by quality, then index
            int ret = Double.compare(quality, n.quality);
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

    // Base strategy (percentile). Also used to model how opponents are expected to
    // apply
    private int[] getSomeApplications(int num, double S, double T, double W, double aptitude, double[] schools,
            double[] synergies) {

        int N = schools.length;
        // Finds schools to apply to
        School[] applications = new School[N];
        for (int i = 0; i < applications.length; i++) {
            // fraction of students I am a better applicant than for this school
            double myPercentile = percentile(aptitude + synergies[i], S, W);
            // fraction of students I need to beat to have a good chance
            // of getting into this school
            // weighted sum of school's quality rank and EV of max synergy among students
            double goalPercentile;
            if (T != 0) {
                double uniPercentile = 1 - i / (N - 1);
                double evMaxSyn = Math.max(0, N - 1 / uniPercentile) / N;
                goalPercentile = (S * uniPercentile + W * evMaxSyn) / (S + W);
            } else
                goalPercentile = N / (N - 1);

            double weight = Math.min(1, myPercentile / goalPercentile);
            applications[i] = new School(i, Math.pow(weight, 0.5) * (weight * schools[i] + synergies[i]));
        }
        Arrays.sort(applications);

        // Sort schools applying to by actual preference
        School[] preferences = new School[num];
        for (int i = 0; i < preferences.length; i++) {
            int uni = applications[i].index;
            preferences[i] = new School(uni, schools[uni] + synergies[uni]);
        }
        Arrays.sort(preferences);

        int[] ret = new int[preferences.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = preferences[i].index;
        }
        return ret;
    }

    public int[] getApplications(int N, double S, double T, double W, double aptitude, List<Double> schools,
            List<Double> synergies) {

        // Convert lists back into primative arrays
        double[] mySynergies = new double[N];
        double[] schoolsArr = new double[N];
        for (int i = 0; i < N; i++) {
            mySynergies[i] = synergies.get(i);
            schoolsArr[i] = schools.get(i);
        }

        if (S == 0 && W == 0) {
            int[] ret = new int[] { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 };
            return ret;
        }
        if (S < MIN_SW * W || T < MIN_TW * W) {
            return getSomeApplications(10, S, T, W, aptitude, schoolsArr, mySynergies);
        }
        // Otherwise, do LK2 strategy:

        // Create array of schools in preference order
        int[] myPrefs = getSomeApplications(N, S, T, W, aptitude, schoolsArr, mySynergies);
        int prefsLength = myPrefs.length;
        int[] myPrefs2 = new int[prefsLength];

        for (int i = 0; i < prefsLength; i++) {
            myPrefs2[i] = myPrefs[i];
        }

        int[] apps = new int[10];

        int[] matchCount = new int[N];
        for (int t = 0; t < TRIALS; t++) {
            int uni = tryApplications(myPrefs, aptitude, schoolsArr, mySynergies, S, T, W);
            if (uni != -1)
                matchCount[uni]++;
        }

        int argMax = 0;
        for (int i = 1; i < N; i++) {
            if (matchCount[i] > matchCount[argMax])
                argMax = i;
        }

        int prefNum = 0;
        for (int i = 0; i < N; i++) {
            if (argMax == myPrefs[i]) {
                prefNum = i;
            }
        }

        if (prefNum <= 6) {
            for (int i = 0; i < 10; i++)
                apps[i] = myPrefs[i];
        } else if (prefNum >= N - 3) {
            for (int i = 0; i < 5; i++)
                apps[i] = myPrefs[N - 18 + i * 3];
            for (int i = 5; i < 10; i++)
                apps[i] = myPrefs[N - 10 + i];
        } else {
            for (int i = 0; i < 10; i++)
                apps[i] = myPrefs[prefNum + i - 6];
        }

        int countApp = 0;
        int[] apps2 = new int[10];
        for (int k = 0; k < prefsLength; k++) {
            for (int j = 0; j < 10; j++) {
                if (apps[j] == myPrefs2[k]) {
                    apps2[countApp] = apps[j];
                    countApp++;
                }
            }
        }
        return apps2;
    }

    // Based on Admissions.runTrial
    // Determines change in utility from not applying to some school in myPrefs
    public int tryApplications(int[] myPrefs, double myAptitude, double[] schools, double[] mySynergies, double S,
            double T, double W) {

        final int N = schools.length;

        // Initialize random variables
        double[] aptitudes = new double[N];
        double[][] synergies = new double[N][N];
        Random rand = new Random();
        for (int i = 0; i < N - 1; ++i) {
            aptitudes[i] = rand.nextDouble() * S;
            for (int j = 0; j < N; ++j) {
                synergies[i][j] = rand.nextDouble() * W;
            }
        }

        // Get each student's choices of schools to which to apply
        int[][] stuPrefs = new int[N][];
        for (int stu = 0; stu < N - 1; ++stu) {
            stuPrefs[stu] = getSomeApplications(OPPONENT_APPS, S, T, W, aptitudes[stu], schools, synergies[stu]);
        }

        // Include my given info
        aptitudes[N - 1] = myAptitude;
        synergies[N - 1] = mySynergies;
        stuPrefs[N - 1] = myPrefs;

        // Build university preference lists filtered by applications
        List<TreeSet<StudentPair>> uniPrefTrees = new ArrayList<TreeSet<StudentPair>>();
        for (int uni = 0; uni < schools.length; ++uni) {
            uniPrefTrees.add(new TreeSet<StudentPair>());
        }
        for (int stu = 0; stu < stuPrefs.length; ++stu) {
            for (int uni : stuPrefs[stu]) {
                uniPrefTrees.get(uni).add(new StudentPair(stu, aptitudes[stu] + synergies[stu][uni]));
            }
        }
        // Need two sets of university preference
        List<List<Integer>> uniPrefs = new ArrayList<List<Integer>>();
        for (TreeSet<StudentPair> prefTree : uniPrefTrees) {
            uniPrefs.add(prefTree.stream().map(StudentPair::getIndex).collect(Collectors.toCollection(ArrayList::new)));
        }

        // Find initial matching
        int[] stuUnis = new int[N];
        int[] uniStus = new int[N];
        for (int i = 0; i < N; ++i) {
            stuUnis[i] = uniStus[i] = -1;
        }
        runMatching(stuUnis, uniStus, stuPrefs, uniPrefs);
        return stuUnis[N - 1];
    }

    // Unmatched universities keep proposing until they run out of applicants
    private void runMatching(int[] stuUnis, int[] uniStus, int[][] stuPrefs, List<List<Integer>> uniPrefs) {
        boolean flag = true;
        while (flag) {
            flag = false;
            for (int uni = 0; uni < uniStus.length; uni++) {
                if (uniStus[uni] == -1 && !uniPrefs.get(uni).isEmpty()) {
                    flag = true;
                    int stu = uniPrefs.get(uni).remove(uniPrefs.get(uni).size() - 1);
                    if (stuUnis[stu] == -1) {
                        stuUnis[stu] = uni;
                        uniStus[uni] = stu;
                    } else if (Arrays.asList(stuPrefs[stu]).indexOf(uni) < Arrays.asList(stuPrefs[stu])
                            .indexOf(stuUnis[stu])) {
                        uniStus[stuUnis[stu]] = -1;
                        stuUnis[stu] = uni;
                        uniStus[uni] = stu;
                    }
                }
            }
        }
    }
}
