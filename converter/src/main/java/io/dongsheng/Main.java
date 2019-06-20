package io.dongsheng;

public class Main {
    public static int foo() {
        return 42;
    }

    public static void main(String[] args) {
        System.out.println(foo());
    }



    /*
1 2
100 3
100 2
100 1
     */
    /*
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int machineNum = scanner.nextInt();
        int jobNum = scanner.nextInt();

        TreeSet<Machine> machines = new TreeSet<>();
        TreeSet<Job> jobs = new TreeSet<>();

        for (int i = 0; i < machineNum; i++) {
            machines.add(new Machine(scanner.nextShort(), scanner.nextShort()));
        }

        for (int i = 0; i < jobNum; i++) {
            jobs.add(new Job(scanner.nextShort(), scanner.nextShort()));
        }

        int num = 0;
        int points = 0;

        for (Job job : jobs) {
            System.out.printf("%d %d\n", job.getTime(), job.getDifficulty());
        }
        System.out.println();

        for (Machine job : machines) {
            System.out.printf("%d %d\n", job.getTime(), job.getDifficulty());
        }

        for(Job job : jobs) {
            Machine m = null;
            for (Machine machine : machines) {
                if (job.getDifficulty() <= machine.getDifficulty() && job.getTime() <= machine.getTime()) {
                    m = machine;
                    break;
                }
            }
            if (m != null) {
                num++;
                points += job.points();
                machines.remove(m);
            }
        }

        System.out.printf("%d %d", num, points);

    }*/
    //find max points
    public Points find(Node[] machines, Node[] jobs) {
        return null;
    }


    public static class Machine extends Node implements Comparable<Machine> {
        public Machine(short time, short difficulty) {
            super(time, difficulty);
        }
        //descending order
        @Override
        public int compareTo(Machine o) {
            int diff = super.getTime() - o.getTime();
            if (diff == 0) {
                return super.difficulty - o.getDifficulty();
            }
            return diff;
        }
    }

    public static class Job extends Node implements Comparable<Job> {
        public Job(short time, short difficulty) {
            super(time, difficulty);
        }
        //descending order
        @Override
        public int compareTo(Job o) {
            int my = 200 * super.time + 3 * super.difficulty;
            int his = 200 * o.getTime() + 3 * o.getDifficulty();
            return his - my;
        }

        public int points() {
            return 200 * super.getTime() + 3 * super.difficulty;
        }
    }


    public static class Node {
        private short time;
        private short difficulty;

        public Node(short time, short difficulty) {
            this.time = time;
            this.difficulty = difficulty;
        }

        public short getTime() {
            return time;
        }

        public void setTime(short time) {
            this.time = time;
        }

        public short getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(short difficulty) {
            this.difficulty = difficulty;
        }
    }


    public static class Points {
        private int jobs;
        private int value;

        public Points(int jobs, int value) {
            this.jobs = jobs;
            this.value = value;
        }
    }
}
