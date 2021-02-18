README.txt: Quick guide to coding up your strategy
Written by Frankie Lam in Spring 2020.

For this assignment you only need to create a file named "Student_netid.java" (replace netid with your netid) containing your strategy. You should not have to edit any other file except students.txt.

A brief tour of what each file does:

Admissions.java - Implements the admissions process
AdmissiosnConfig.java - Initialises "meta-variables" S, T, and W. 
Student_*.java - Sample strategies
Student.java - the interface that your "Student_netid.java" must implement.
Tournament.java - A tournament infrastucture that will be the same across Strategy Designs.
students.txt - A list of all the contesting strategies that are applying. This is so that you can test your strategies against each other and the sample strategies. 

Again, you do not have to edit or understand any of these files except Student.java and students.txt.

To start, read the comments in Student.java, and remind yourself what are the inputs to your strategy. Then, take a look at the sample strategies, for example Student_usnews.java. Note how you are expected to return a list of 10 indices corresponding to applications in decreasing order of preference. Relatively sophisticated strategies such as Studnet_holist.java should give you a good idea how to specify and sort schools by an arbitrary function.

After you coded up your strategy, test it out by editting students.txt. List all the strategies you want to run, then call "make test". Note you can have multiple copies of the same strategy. Results are printed on screen as well as saved in "results.csv".

You can also upload your strategy to a leaderboard via tigerfile (coming soon, as of writing) and see how your strategy performs relative to everyone in the class. This is for your reference only - grading will be independent of the leaderboard!

Have fun!