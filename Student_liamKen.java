// Student_liamdj.java:
// Created by Liam Johansson

import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Student_liamKen implements Student {

    private static final int TRIALS = 200;
    private static final int OPPONENT_APPS = 10;

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
        int[] myPrefs = getSomeApplications(N, S, T, W, aptitude, schoolsArr, mySynergies);

        int[] apps = new int[10];

        // Maintain array of which applications gain most EV
        for (int k = 0; k < 10; k++) {
            int[] matchCount = new int[N];
            for (int t = 0; t < TRIALS; t++) {
                int uni = tryApplications(myPrefs, aptitude, schoolsArr, mySynergies, S, T, W);
                if (uni != -1)
                    matchCount[uni]++;
            }

            int argMax = 0;
            for (int i = 0; i < N; i++) {
                if (matchCount[i] > matchCount[argMax])
                    argMax = i;
            }

            apps[k] = argMax;

            // Creates new preference list with one less school
            int[] newPrefs = new int[myPrefs.length - 1];
            int offset = 0;
            for (int i = 0; i < newPrefs.length; i++) {
                if (argMax == myPrefs[i])
                    offset = 1;
                newPrefs[i] = myPrefs[i + offset];
            }
            myPrefs = newPrefs;
        }

        return apps;
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
