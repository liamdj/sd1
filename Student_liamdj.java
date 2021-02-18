// Student_liamdj.java:
// Created by Liam Johansson

import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Student_liamdj implements Student {

    private static final int TRIALS = 140;
    private static final double SIM_DECAY = 0.33;

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

    private static class SkipAppResult {

        public int uni;
        public int diff;

        public SkipAppResult(int uni, int diff) {
            this.uni = uni;
            this.diff = diff;
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

    // Models how opponents are expected to apply
    // Mix between holist and synergist depending on how qualified a student is
    private int[] getSomeApplications(int num, double S, double T, double W, double aptitude, double[] schools,
            double[] synergies) {

        // Finds schools to apply to
        School[] preferences = new School[schools.length];
        for (int i = 0; i < preferences.length; i++) {
            preferences[i] = new School(i, (aptitude + synergies[i]) / (S + W) * schools[i] + synergies[i]);
        }
        Arrays.sort(preferences);

        // Sort schools applying to by actual preference
        School[] willApply = new School[num];
        for (int i = 0; i < num; i++) {
            int uni = preferences[i].index;
            willApply[i] = new School(uni, schools[uni] + synergies[uni]);
        }
        Arrays.sort(willApply);

        int[] ret = new int[num];
        for (int i = 0; i < num; i++) {
            ret[i] = willApply[i].index;
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

        // Create array of schools in preference order
        int[] apps = getSomeApplications(N, S, T, W, aptitude, schoolsArr, mySynergies);

        // Maintain array of which applications gain most EV
        int[] evDiffs = new int[N];
        while (apps.length > 10) {
            // Reduce calculations for previous trials with different sets of application
            // for (int i = 0; i < N; i++) {
            // evDiffs[i] *= 1 - SIM_DECAY;
            // }
            evDiffs = new int[N];
            // Repeated simulate situation and keep track of EV difference for each
            // application
            System.err.println("--------------" + apps.length);
            for (int t = 0; t < TRIALS; t++) {
                SkipAppResult res = skipApplication(apps, aptitude, schoolsArr, mySynergies, S, T, W);
                if (res != null && evDiffs[res.uni] == 0) {
                    System.err.println(res.uni);
                }
                if (res != null)
                    evDiffs[res.uni] += res.diff;
            }
            // Determine school of remaining with least benefit to apply
            int worstApp = apps[0];
            for (int i = 1; i < apps.length; i++) {
                if (evDiffs[apps[i]] > evDiffs[worstApp])
                    worstApp = apps[i];
            }
            System.err.println("[" + worstApp + "]");

            // Creates new preference list with one less school
            int len = apps.length - 1;
            int[] newApps = new int[len];
            int offset = 0;
            for (int i = 0; i < newApps.length; i++) {
                if (worstApp == apps[i])
                    offset = 1;
                newApps[i] = apps[i + offset];
                System.err.print(newApps[i] + ", ");
            }
            apps = newApps;
            System.err.println();
        }

        return apps;
    }

    // Based on Admissions.runTrial
    // Determines change in utility from not applying to some school in myPrefs
    public SkipAppResult skipApplication(int[] myPrefs, double myAptitude, double[] schools, double[] mySynergies,
            double S, double T, double W) {

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
            stuPrefs[stu] = getSomeApplications(12, S, T, W, aptitudes[stu], schools, synergies[stu]);
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
        List<List<Integer>> uniPrefsDup = new ArrayList<List<Integer>>();
        for (TreeSet<StudentPair> prefTree : uniPrefTrees) {
            uniPrefs.add(prefTree.stream().map(StudentPair::getIndex).collect(Collectors.toCollection(ArrayList::new)));
            uniPrefsDup
                    .add(prefTree.stream().map(StudentPair::getIndex).collect(Collectors.toCollection(ArrayList::new)));
        }

        // Find initial matching
        int[] stuUnis = new int[N];
        int[] uniStus = new int[N];
        for (int i = 0; i < N; ++i) {
            stuUnis[i] = uniStus[i] = -1;
        }
        runMatching(stuUnis, uniStus, stuPrefs, uniPrefs);
        int match = stuUnis[N - 1];
        int util = stuUtility(match, schools, mySynergies);

        if (match == -1)
            return null;

        // Re-run matching with you NOT applying to the university you were just
        // matched with
        for (int i = 0; i < N; ++i) {
            stuUnis[i] = uniStus[i] = -1;
        }
        uniPrefsDup.get(match).remove((Integer) (N - 1));
        runMatching(stuUnis, uniStus, stuPrefs, uniPrefsDup);
        int utilAfter = stuUtility(stuUnis[N - 1], schools, mySynergies);

        // Return utility difference after not applying to initial match
        return new SkipAppResult(match, utilAfter - util);
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

    // 1 utility for every school a student weakly prefer their result to
    private int stuUtility(int uni, double[] schools, double[] synergies) {
        if (uni == -1)
            return 0;

        int ret = 0;
        double res = schools[uni] + synergies[uni];
        for (int u = 0; u < schools.length; ++u) {
            if (schools[u] + synergies[u] <= res) {
                ++ret;
            }
        }
        return ret;
    }

    public static void main(String[] args) {
        final int N = 56;
        Random rand = new Random();
        double aptitude = rand.nextDouble() * 100;
        List<Double> schools = new ArrayList<Double>();
        List<Double> synergies = new ArrayList<Double>();
        for (int i = 0; i < N; ++i) {
            schools.add(rand.nextDouble() * 100);
            synergies.add(rand.nextDouble() * 10);
        }
        Collections.sort(schools);
        int[] arr = new Student_liamdj().getApplications(N, 100, 100, 10, aptitude, schools, synergies);
        for (int i = 0; i < arr.length; i++)
            System.out.print(arr[i] + " ");
        System.out.println();
    }

}
