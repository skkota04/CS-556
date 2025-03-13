import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Q3 {
    private static final int ARRIVALS = 500; // number of customer arrivals to simulate
    private static final int SIMULATIONS = 1000; // number of simulation runs

    // Class for storing customer details
    static class Customer {
        double arrivalTime;
        double serviceStartTime;
        double serviceTime;
        double departureTime;

        Customer(double arrivalTime) {
            this.arrivalTime = arrivalTime;
        }
    }

    // Class for storing simulation results
    static class SimulationResults {
        double avgWaitingTime;
        double avgSystemTime;
        double utilizationFactor;
        double idleTimeFraction;
        double avgQueueLength;
        int maxQueueLength;
        double emptyQueueProbability;
    }

    private Random random;
    private double lambda; // arrival rate
    private double mu;    // service rate

    // Constructor to initialize parameters
    public Q3(double lambda, double mu) {
        this.random = new Random();
        this.lambda = lambda;
        this.mu = mu;
    }

    // Generate exponential random variable
    private double getExponential(double rate) {
        return -Math.log(1.0 - random.nextDouble()) / rate;
    }

    private SimulationResults runSimulation() {
        Queue<Customer> queue = new LinkedList<>();
        ArrayList<Customer> completedCustomers = new ArrayList<>();
        
        double currentTime = 0.0;
        double nextArrival = getExponential(lambda);
        double nextDeparture = Double.MAX_VALUE;
        double busyTime = 0.0;
        double queueLengthTimeProduct = 0.0;
        double emptyQueueTime = 0.0;
        double lastEventTime = 0.0;
        int totalArrivals = 0;
        int maxQueueLength = 0;

        // Process arrivals until 500 customers have arrived
        while (totalArrivals < ARRIVALS) {
            // Handle arrival
            if (nextArrival < nextDeparture) {
                currentTime = nextArrival;
                totalArrivals++;

                // Update queue length time product and empty queue time
                queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);
                if (queue.isEmpty()) {
                    emptyQueueTime += (currentTime - lastEventTime);
                }

                Customer customer = new Customer(currentTime);
                queue.add(customer);

                // Update maximum queue length
                maxQueueLength = Math.max(maxQueueLength, queue.size());

                // If this is the only customer, start service
                if (queue.size() == 1) {
                    customer.serviceStartTime = currentTime;
                    customer.serviceTime = getExponential(mu);
                    customer.departureTime = currentTime + customer.serviceTime;
                    nextDeparture = customer.departureTime;
                }

                nextArrival = currentTime + getExponential(lambda);
                lastEventTime = currentTime;
            }
            // Handle departure
            else {
                currentTime = nextDeparture;

                // Update queue length time product and empty queue time
                queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);
                if (queue.isEmpty()) {
                    emptyQueueTime += (currentTime - lastEventTime);
                }

                Customer served = queue.poll();
                completedCustomers.add(served);
                busyTime += served.serviceTime;

                // If there are more customers, start serving next
                if (!queue.isEmpty()) {
                    Customer nextCustomer = queue.peek();
                    nextCustomer.serviceStartTime = currentTime;
                    nextCustomer.serviceTime = getExponential(mu);
                    nextCustomer.departureTime = currentTime + nextCustomer.serviceTime;
                    nextDeparture = nextCustomer.departureTime;
                } else {
                    nextDeparture = Double.MAX_VALUE;
                }
                lastEventTime = currentTime;
            }
        }

        // Process remaining customers in the queue
        while (!queue.isEmpty()) {
            currentTime = nextDeparture;

            // Update queue length time product and empty queue time
            queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);
            if (queue.isEmpty()) {
                emptyQueueTime += (currentTime - lastEventTime);
            }

            Customer served = queue.poll();
            completedCustomers.add(served);
            busyTime += served.serviceTime;

            if (!queue.isEmpty()) {
                Customer nextCustomer = queue.peek();
                nextCustomer.serviceStartTime = currentTime;
                nextCustomer.serviceTime = getExponential(mu);
                nextCustomer.departureTime = currentTime + nextCustomer.serviceTime;
                nextDeparture = nextCustomer.departureTime;
            } else {
                nextDeparture = Double.MAX_VALUE;
            }
            lastEventTime = currentTime;
        }

        // Calculate total simulation time (time until last customer departs)
        double totalSimulationTime = currentTime;

        // Calculate performance measures
        SimulationResults results = new SimulationResults();
        double totalWaitingTime = 0.0;
        double totalSystemTime = 0.0;
        for (Customer c : completedCustomers) {
            totalWaitingTime += (c.serviceStartTime - c.arrivalTime);
            totalSystemTime += (c.departureTime - c.arrivalTime);
        }

        results.avgWaitingTime = totalWaitingTime / completedCustomers.size();
        results.avgSystemTime = totalSystemTime / completedCustomers.size();
        results.utilizationFactor = busyTime / totalSimulationTime;
        results.idleTimeFraction = 1.0 - results.utilizationFactor;
        results.avgQueueLength = queueLengthTimeProduct / totalSimulationTime;
        results.maxQueueLength = maxQueueLength;
        results.emptyQueueProbability = emptyQueueTime / totalSimulationTime;

        return results;
    }

    private SimulationResults runMultipleSimulations() {
        SimulationResults avgResults = new SimulationResults();
        int validSimulations = 0;

        for (int i = 0; i < SIMULATIONS; i++) {
            SimulationResults results = runSimulation();
            avgResults.avgWaitingTime += results.avgWaitingTime;
            avgResults.avgSystemTime += results.avgSystemTime;
            avgResults.utilizationFactor += results.utilizationFactor;
            avgResults.idleTimeFraction += results.idleTimeFraction;
            avgResults.avgQueueLength += results.avgQueueLength;
            avgResults.maxQueueLength = Math.max(avgResults.maxQueueLength, results.maxQueueLength);
            avgResults.emptyQueueProbability += results.emptyQueueProbability;
            validSimulations++;
        }

        // Average the results
        avgResults.avgWaitingTime /= validSimulations;
        avgResults.avgSystemTime /= validSimulations;
        avgResults.utilizationFactor /= validSimulations;
        avgResults.idleTimeFraction /= validSimulations;
        avgResults.avgQueueLength /= validSimulations;
        avgResults.emptyQueueProbability /= validSimulations;

        return avgResults;
    }

    public static void runSimulationAnalysis(double lambda, double mu) {
        System.out.println("\nCoffee Shop Simulation Results (Averaged over " + SIMULATIONS + " runs):");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-25s %-15s %-15s %-15s %-15s %-15s %-15s\n",
                "Metric", "Avg Wait Time", "Avg Sys Time", "Utilization", "Idle Fraction", "Avg Queue Len", "Max Queue Len", "P(Empty Queue)");

        Q3 simulation = new Q3(lambda, mu);
        SimulationResults results = simulation.runMultipleSimulations();

        System.out.printf("%-25s %-15.6f %-15.6f %-15.6f %-15.6f %-15.6f %-15d %-15.6f\n",
                "Simulation Results",
                results.avgWaitingTime,
                results.avgSystemTime,
                results.utilizationFactor,
                results.idleTimeFraction,
                results.avgQueueLength,
                results.maxQueueLength,
                results.emptyQueueProbability);

        // Theoretical values for comparison
        System.out.println("\nTheoretical Values (for comparison):");
        System.out.println("--------------------------------------------------");
        double rho = lambda / mu;
        double Lq = (lambda * lambda) / (mu * (mu - lambda)); // Average number in queue
        double Wq = Lq / lambda; // Average waiting time in queue
        double W = Wq + (1.0 / mu); // Average time in system
        double P0 = 1.0 - rho; // Probability of empty system

        System.out.printf("%-25s %-15.6f %-15.6f %-15.6f %-15.6f %-15.6f %-15s %-15.6f\n",
                "Theoretical Values",
                Wq,
                W,
                rho,
                1.0 - rho,
                Lq,
                "N/A",
                P0);
    }

    public static void main(String[] args) {
        double lambda = 10.0; // customers per hour
        double mu = 15.0;     // customers per hour
        runSimulationAnalysis(lambda, mu);
    }
}