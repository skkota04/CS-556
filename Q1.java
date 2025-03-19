import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Q2 {
    private static final double SIMULATION_TIME = 8.0; // hours

    static class Customer {
        double arrivalTime;
        double serviceStartTime;
        double serviceTime;
        double departureTime;
        int serverId; // Track which server handled this customer

        Customer(double arrivalTime) {
            this.arrivalTime = arrivalTime;
        }
    }

    static class Server {
        boolean isBusy;
        Customer currentCustomer;
        double busyUntil;

        Server() {
            isBusy = false;
            busyUntil = 0.0;
        }
    }

    static class PeriodResults {
        double totalWaitingTime;
        double totalSystemTime;
        double totalBusyTime;
        double queueLengthTimeProduct;
        double allBusyTime;
        int completedCustomers;
        double periodDuration;

        PeriodResults(double periodDuration) {
            this.totalWaitingTime = 0.0;
            this.totalSystemTime = 0.0;
            this.totalBusyTime = 0.0;
            this.queueLengthTimeProduct = 0.0;
            this.allBusyTime = 0.0;
            this.completedCustomers = 0;
            this.periodDuration = periodDuration;
        }

        void addCustomer(Customer c) {
            totalWaitingTime += c.serviceStartTime - c.arrivalTime;
            totalSystemTime += c.departureTime - c.arrivalTime;
            completedCustomers++;
        }

        void addBusyTime(double time) {
            totalBusyTime += time;
        }

        void addQueueLengthTime(double queueLength, double time) {
            queueLengthTimeProduct += queueLength * time;
        }

        void addAllBusyTime(double time) {
            allBusyTime += time;
        }

        double getAvgWaitingTime() {
            return completedCustomers > 0 ? totalWaitingTime / completedCustomers : 0.0;
        }

        double getAvgSystemTime() {
            return completedCustomers > 0 ? totalSystemTime / completedCustomers : 0.0;
        }

        double getUtilizationRate(int servers) {
            return totalBusyTime / (periodDuration * servers);
        }

        double getAvgQueueLength() {
            return queueLengthTimeProduct / periodDuration;
        }

        double getProbAllBusy() {
            return allBusyTime / periodDuration;
        }
    }

    private Random random;
    private double lambda; // arrival rate
    private double mu;     // service rate per server
    private int maxServers; // maximum number of servers (used to initialize server array)

    public Q2(double lambda, double mu) {
        this.random = new Random();
        this.lambda = lambda;
        this.mu = mu;
        this.maxServers = 4; // maximum number of servers needed at any point
    }

    private double getExponential(double rate) {
        return -Math.log(1.0 - random.nextDouble()) / rate;
    }

    // Determine number of active servers based on current time
    private int getActiveServers(double currentTime) {
        if (currentTime < 2.0) {
            return 2; // First 2 hours: 2 servers
        } else if (currentTime < 5.0) {
            return 4; // Next 3 hours: 4 servers
        } else {
            return 3; // Last 3 hours: 3 servers
        }
    }

    // Determine which period the time falls into
    private int getPeriod(double time) {
        if (time < 2.0) return 0; // Period 1: 0-2 hours
        else if (time < 5.0) return 1; // Period 2: 2-5 hours
        else return 2; // Period 3: 5-8 hours
    }

    private void runSimulation() {
        Queue<Customer> queue = new LinkedList<>();
        ArrayList<Customer> completedCustomers = new ArrayList<>();
        Server[] servers = new Server[maxServers];
        for (int i = 0; i < maxServers; i++) {
            servers[i] = new Server();
        }

        // Initialize results for three periods and the entire day.
        PeriodResults[] periodResults = new PeriodResults[3];
        periodResults[0] = new PeriodResults(2.0); // 0-2 hours
        periodResults[1] = new PeriodResults(3.0); // 2-5 hours
        periodResults[2] = new PeriodResults(3.0); // 5-8 hours
        PeriodResults entireDayResults = new PeriodResults(SIMULATION_TIME);

        double currentTime = 0.0;
        double nextArrival = getExponential(lambda);
        double lastEventTime = currentTime;
        int totalArrivals = 0;

        while (currentTime < SIMULATION_TIME) {
            int activeServers = getActiveServers(currentTime);
            int currentPeriod = getPeriod(currentTime);

            // Find earliest departure among active servers
            double nextDeparture = Double.MAX_VALUE;
            int departingServer = -1;
            for (int i = 0; i < activeServers; i++) {
                if (servers[i].isBusy && servers[i].busyUntil < nextDeparture) {
                    nextDeparture = servers[i].busyUntil;
                    departingServer = i;
                }
            }

            // Determine next event: arrival or departure
            double nextEventTime;
            boolean isArrivalEvent;
            if (nextArrival < nextDeparture) {
                nextEventTime = nextArrival;
                isArrivalEvent = true;
            } else {
                nextEventTime = nextDeparture;
                isArrivalEvent = false;
            }

            // Update metrics over the interval from the current time to next event
            double timeInterval = nextEventTime - currentTime;
            if (timeInterval > 0) {
                periodResults[currentPeriod].addQueueLengthTime(queue.size(), timeInterval);
                entireDayResults.addQueueLengthTime(queue.size(), timeInterval);

                int busyServers = countBusyServers(servers, activeServers);
                if (busyServers == activeServers) {
                    periodResults[currentPeriod].addAllBusyTime(timeInterval);
                    entireDayResults.addAllBusyTime(timeInterval);
                }
            }

            // Advance current time to the next event
            currentTime = nextEventTime;
            lastEventTime = currentTime;

            if (isArrivalEvent) {
                // Process arrival event
                totalArrivals++;
                Customer customer = new Customer(currentTime);
                int availableServer = findAvailableServer(servers, activeServers);
                if (availableServer != -1) {
                    // Server is available; begin service immediately.
                    customer.serviceStartTime = currentTime;
                    customer.serviceTime = getExponential(mu);
                    customer.departureTime = currentTime + customer.serviceTime;
                    customer.serverId = availableServer;

                    servers[availableServer].isBusy = true;
                    servers[availableServer].currentCustomer = customer;
                    servers[availableServer].busyUntil = customer.departureTime;
                } else {
                    // No server is available; add customer to queue.
                    queue.add(customer);
                }
                nextArrival = currentTime + getExponential(lambda);
            } else {
                // Process departure event
                Customer served = servers[departingServer].currentCustomer;
                completedCustomers.add(served);
                int departurePeriod = getPeriod(served.departureTime);
                periodResults[departurePeriod].addCustomer(served);
                entireDayResults.addCustomer(served);
                periodResults[departurePeriod].addBusyTime(served.serviceTime);
                entireDayResults.addBusyTime(served.serviceTime);

                // Free the server.
                servers[departingServer].isBusy = false;
                servers[departingServer].currentCustomer = null;

                // If there is a waiting customer, assign them to an available server.
                if (!queue.isEmpty()) {
                    Customer nextCustomer = queue.poll();
                    int availableServer = findAvailableServer(servers, activeServers);
                    if (availableServer != -1) {
                        nextCustomer.serviceStartTime = currentTime;
                        nextCustomer.serviceTime = getExponential(mu);
                        nextCustomer.departureTime = currentTime + nextCustomer.serviceTime;
                        nextCustomer.serverId = availableServer;

                        servers[availableServer].isBusy = true;
                        servers[availableServer].currentCustomer = nextCustomer;
                        servers[availableServer].busyUntil = nextCustomer.departureTime;
                    } else {
                        // This situation is unlikely immediately after a departure,
                        // but if it happens, return the customer to the queue.
                        queue.add(nextCustomer);
                    }
                }
            }

            // When active servers decrease, move customers from deactivated servers to the queue.
            for (int i = activeServers; i < maxServers; i++) {
                if (servers[i].isBusy) {
                    Customer interrupted = servers[i].currentCustomer;
                    servers[i].isBusy = false;
                    servers[i].currentCustomer = null;
                    queue.add(interrupted);
                }
            }
        }

        // Print results for each period
        System.out.println("\nSimulation Results (with changing servers and infinite queue):");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-20s %-15s %-15s %-15s %-15s %-15s%n",
                "Period", "Avg Wait Time", "Avg Sys Time", "Utilization", "Avg Queue Len", "P(All Busy)");

        for (int i = 0; i < periodResults.length; i++) {
            String periodLabel;
            int serverCount; // Rename to avoid conflict with servers array
            if (i == 0) {
                periodLabel = "0-2 hours";
                serverCount = 2;
            } else if (i == 1) {
                periodLabel = "2-5 hours";
                serverCount = 4;
            } else {
                periodLabel = "5-8 hours";
                serverCount = 3;
            }
            System.out.printf("%-20s %-15.6f %-15.6f %-15.6f %-15.6f %-15.6f%n",
                    periodLabel,
                    periodResults[i].getAvgWaitingTime(),
                    periodResults[i].getAvgSystemTime(),
                    periodResults[i].getUtilizationRate(serverCount),
                    periodResults[i].getAvgQueueLength(),
                    periodResults[i].getProbAllBusy());
        }

        // Print results for the entire day
        System.out.println("\nEntire Day Results:");
        System.out.printf("%-20s %-15s %-15s %-15s %-15s %-15s%n",
                "Period", "Avg Wait Time", "Avg Sys Time", "Utilization", "Avg Queue Len", "P(All Busy)");
        // Total server-hours = (2 hours * 2) + (3 hours * 4) + (3 hours * 3) = 4 + 12 + 9 = 25 server-hours
        double entireDayUtilization = entireDayResults.totalBusyTime / 25.0;
        System.out.printf("%-20s %-15.6f %-15.6f %-15.6f %-15.6f %-15.6f%n",
                "0-8 hours",
                entireDayResults.getAvgWaitingTime(),
                entireDayResults.getAvgSystemTime(),
                entireDayUtilization,
                entireDayResults.getAvgQueueLength(),
                entireDayResults.getProbAllBusy());
    }

    private int findAvailableServer(Server[] servers, int activeServers) {
        for (int i = 0; i < activeServers; i++) {
            if (!servers[i].isBusy) {
                return i;
            }
        }
        return -1;
    }

    private int countBusyServers(Server[] servers, int activeServers) {
        int busyCount = 0;
        for (int i = 0; i < activeServers; i++) {
            if (servers[i].isBusy) {
                busyCount++;
            }
        }
        return busyCount;
    }

    public static void analyzeServerEffect(double lambda, double mu) {
        Q2 simulation = new Q2(lambda, mu);
        simulation.runSimulation();
    }

    public static void main(String[] args) {
        double lambda = 40.0;  // arrival rate
        double mu = 15.0;      // service rate per server

        // Run the simulation with changing servers
        analyzeServerEffect(lambda, mu);
    }
}
