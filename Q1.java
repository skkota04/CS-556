import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Q1 {
    private static final double SIMULATION_TIME = 3.0; // hours

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

    static class SimulationResults {
        double avgWaitingTime;
        double avgSystemTime;
        double utilizationRate;
        double avgQueueLength;
    }

    private Random random;
    private double lambda; // arrival rate
    private double mu;    // service rate
    private int numServers; // number of servers

    public Q1(double lambda, double mu, int numServers) {
        this.random = new Random();
        this.lambda = lambda;
        this.mu = mu;
        this.numServers = numServers;
    }

    private double getExponential(double rate) {
        return -Math.log(1.0 - random.nextDouble()) / rate;
    }

    private SimulationResults runSimulation() {
        Queue<Customer> queue = new LinkedList<>();
        ArrayList<Customer> completedCustomers = new ArrayList<>();
        Server[] servers = new Server[numServers];
        
        // Initialize servers
        for (int i = 0; i < numServers; i++) {
            servers[i] = new Server();
        }

        double currentTime = 0.0;
        double nextArrival = getExponential(lambda);
        
        int totalArrivals = 0;
        double totalBusyTime = 0.0;
        double queueLengthTimeProduct = 0.0;
        double lastEventTime = 0.0;

        while (currentTime < SIMULATION_TIME) {
            // Find next event time (either arrival or earliest departure)
            double nextDeparture = Double.MAX_VALUE;
            int departingServer = -1;
            
            // Find earliest departure time among all servers
            for (int i = 0; i < numServers; i++) {
                if (servers[i].isBusy && servers[i].busyUntil < nextDeparture) {
                    nextDeparture = servers[i].busyUntil;
                    departingServer = i;
                }
            }

            if (nextArrival < nextDeparture) {
                // Handle arrival
                currentTime = nextArrival;
                totalArrivals++;

                // Update queue length time product
                queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);

                // Create new customer
                Customer customer = new Customer(currentTime);
                
                // Try to find an available server
                int availableServer = findAvailableServer(servers);
                if (availableServer != -1) {
                    // Assign customer to available server
                    customer.serviceStartTime = currentTime;
                    customer.serviceTime = getExponential(mu);
                    customer.departureTime = currentTime + customer.serviceTime;
                    customer.serverId = availableServer;
                    
                    servers[availableServer].isBusy = true;
                    servers[availableServer].currentCustomer = customer;
                    servers[availableServer].busyUntil = customer.departureTime;
                } else {
                    // Add to queue if no server available (no capacity check needed)
                    queue.add(customer);
                }
                nextArrival = currentTime + getExponential(lambda);
                lastEventTime = currentTime;
            } else {
                // Handle departure
                currentTime = nextDeparture;
                
                // Update queue length time product
                queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);

                // Complete service for the departing customer
                Customer served = servers[departingServer].currentCustomer;
                completedCustomers.add(served);
                totalBusyTime += served.serviceTime;
                
                // Free up the server
                servers[departingServer].isBusy = false;
                servers[departingServer].currentCustomer = null;

                // If there are customers in queue, serve next customer
                if (!queue.isEmpty()) {
                    Customer nextCustomer = queue.poll();
                    nextCustomer.serviceStartTime = currentTime;
                    nextCustomer.serviceTime = getExponential(mu);
                    nextCustomer.departureTime = currentTime + nextCustomer.serviceTime;
                    nextCustomer.serverId = departingServer;
                    
                    servers[departingServer].isBusy = true;
                    servers[departingServer].currentCustomer = nextCustomer;
                    servers[departingServer].busyUntil = nextCustomer.departureTime;
                }
                lastEventTime = currentTime;
            }
        }

        // Calculate the performance measures
        SimulationResults results = new SimulationResults();
        double totalWaitingTime = 0.0;
        double totalSystemTime = 0.0;
        for (Customer c : completedCustomers) {
            totalWaitingTime += c.serviceStartTime - c.arrivalTime;
            totalSystemTime += c.departureTime - c.arrivalTime;
        }
        results.avgWaitingTime = totalWaitingTime / completedCustomers.size();
        results.avgSystemTime = totalSystemTime / completedCustomers.size();
        results.utilizationRate = totalBusyTime / (currentTime * numServers); // Average utilization across all servers
        results.avgQueueLength = queueLengthTimeProduct / currentTime;
        return results;
    }

    private int findAvailableServer(Server[] servers) {
        for (int i = 0; i < servers.length; i++) {
            if (!servers[i].isBusy) {
                return i;
            }
        }
        return -1;
    }

    public static void analyzeServerEffect(double lambda, double mu, int minServers, int maxServers) {
        System.out.println("\nServer Analysis Results (with infinite queue):");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-10s %-15s %-15s %-15s %-15s%n",
                "Servers", "Avg Wait Time", "Avg Sys Time", "Utilization", "Avg Queue Len");
        for (int servers = minServers; servers <= maxServers; servers++) {
            Q1 simulation = new Q1(lambda, mu, servers);
            SimulationResults results = simulation.runSimulation(); // Use single simulation run

            System.out.printf("%-10d %-15.6f %-15.6f %-15.6f %-15.6f%n",
                    servers,
                    results.avgWaitingTime,
                    results.avgSystemTime,
                    results.utilizationRate,
                    results.avgQueueLength);
        }
    }

    public static void main(String[] args) {
        double lambda = 40.0;  // arrival rate
        double mu = 15.0;     // service rate per server

        // Analyze effect of number of servers with infinite queue
        analyzeServerEffect(lambda, mu, 1, 4);
    }
}