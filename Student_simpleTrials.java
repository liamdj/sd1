// Student_liamdj.java:
// Created by Liam Johansson

import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Student_simpleTrials implements Student {

    private static final int TRIALS = 5000;
    private static final int OPPONENT_APPLICATIONS = 30;

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
    // drawn on [0, A] and [0, B]
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

    // Models how opponents are expected to apply
    // Mix between holist and synergist depending on how qualified a student is
    private int[] getSomeApplications(int num, double S, double T, double W, double aptitude, double[] schools,
            double[] synergies) {

        // Finds schools to apply to
        School[] applications = new School[schools.length];
        for (int i = 0; i < applications.length; i++) {
            double myPercentile = percentile(aptitude + synergies[i], S, W);
            double uniPercentile = schools[i] / T;
            double weight = 1 + 1 * Math.min(0, myPercentile - uniPercentile);
            applications[i] = new School(i, weight * schools[i] + synergies[i]);
        }
        Arrays.sort(applications);

        // Sort schools applying to by actual preference
        School[] preferences = new School[num];
        for (int i = 0; i < num; i++) {
            int uni = applications[i].index;
            preferences[i] = new School(uni, schools[uni] + synergies[uni]);
        }
        Arrays.sort(preferences);

        int[] ret = new int[num];
        for (int i = 0; i < num; i++) {
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

        // Create array of schools in preference order
        // System.err.println(aptitude / S);
        int[] apps = getSomeApplications(N, S, T, W, aptitude, schoolsArr, mySynergies);
        Random rand = new Random();

        int[] matches = new int[N];
        for (int t = 0; t < TRIALS; t++) {
            int uni = sendApplications(apps, aptitude, schoolsArr, mySynergies, S, T, W, rand);
            if (uni != -1)
                matches[uni]++;
        }

        // multiply matches by utility
        for (int i = 0; i < N; i++) {
            matches[apps[i]] *= N - i;
        }
        int[] utilities = matches.clone();
        Arrays.sort(utilities);

        int worstToKeep = utilities[N - 10];
        // if (worstToKeep <= 0)
        // for (int i = 0; i < N; i++)
        // System.err.print(utilities[i] + " ");
        int[] ret = new int[10];
        for (int i = 0, k = 0; i < 10; k++) {
            if (matches[apps[k]] >= worstToKeep) {
                ret[i++] = apps[k];
            }
        }

        return ret;
    }

    // Based on Admissions.runTrial
    // Determines match nt sendAp
    private int sendApplications(int[] myPrefs, double myAptitude, double[] schools, double[] mySynergies, double S,
            double T, double W, Random rand) {

        final int N = schools.length;

        // Initialize random variables
        double[] aptitudes = new double[N];
        double[][] synergies = new double[N][N];
        for (int i = 0; i < N - 1; ++i) {
            aptitudes[i] = rand.nextDouble() * S;
            for (int j = 0; j < N; ++j) {
                synergies[i][j] = rand.nextDouble() * W;
            }
        }

        // Get each student's choices of schools to which to apply
        int[][] stuPrefs = new int[N][];
        for (int stu = 0; stu < N - 1; ++stu) {
            stuPrefs[stu] = getSomeApplications(OPPONENT_APPLICATIONS, S, T, W, aptitudes[stu], schools,
                    synergies[stu]);
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
            List<Integer> toAdd = prefTree.stream().map(StudentPair::getIndex)
                    .collect(Collectors.toCollection(ArrayList::new));
            uniPrefs.add(toAdd);
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
