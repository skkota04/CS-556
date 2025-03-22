import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Q4 {
    private static final int ARRIVALS = 500; // number of customer arrivals to simulate
    private static final int SIMULATIONS = 1; // number of simulation runs
    private static final double MAX_WAIT_TIME = 5.0 / 60.0; // 5 minutes in hours

    // Class for storing customer details
    static class Customer {
        double arrivalTime;
        double serviceStartTime;
        double serviceTime;
        double departureTime;
        boolean served;

        Customer(double arrivalTime) {
            this.arrivalTime = arrivalTime;
            this.served = false;
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
        int customersLost; // Added to track customers who left due to long wait
    }

    private Random random;
    private double lambda; // arrival rate
    private double mu;    // service rate

    public Q4(double lambda, double mu) {
        this.random = new Random();
        this.lambda = lambda;
        this.mu = mu;
    }

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
        int customersLost = 0;

        while (totalArrivals < ARRIVALS) {
            if (nextArrival < nextDeparture) {
                currentTime = nextArrival;
                totalArrivals++;

                queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);
                if (queue.isEmpty()) {
                    emptyQueueTime += (currentTime - lastEventTime);
                }

                Customer customer = new Customer(currentTime);
                queue.add(customer);
                maxQueueLength = Math.max(maxQueueLength, queue.size());

                if (queue.size() == 1) {
                    if (currentTime - customer.arrivalTime <= MAX_WAIT_TIME) {
                        customer.serviceStartTime = currentTime;
                        customer.serviceTime = getExponential(mu);
                        customer.departureTime = currentTime + customer.serviceTime;
                        customer.served = true;
                        nextDeparture = customer.departureTime;
                    } else {
                        // Customer leaves due to excessive wait
                        queue.poll();
                        customersLost++;
                    }
                }

                nextArrival = currentTime + getExponential(lambda);
                lastEventTime = currentTime;
            } else {
                currentTime = nextDeparture;

                queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);
                if (queue.isEmpty()) {
                    emptyQueueTime += (currentTime - lastEventTime);
                }

                Customer served = queue.peek();
                if (served.served) {
                    queue.poll();
                    completedCustomers.add(served);
                    busyTime += served.serviceTime;
                }

                if (!queue.isEmpty()) {
                    Customer nextCustomer = queue.peek();
                    if (currentTime - nextCustomer.arrivalTime <= MAX_WAIT_TIME) {
                        nextCustomer.serviceStartTime = currentTime;
                        nextCustomer.serviceTime = getExponential(mu);
                        nextCustomer.departureTime = currentTime + nextCustomer.serviceTime;
                        nextCustomer.served = true;
                        nextDeparture = nextCustomer.departureTime;
                    } else {
                        // Customer leaves due to excessive wait
                        queue.poll();
                        customersLost++;
                        nextDeparture = queue.isEmpty() ? Double.MAX_VALUE : currentTime;
                    }
                } else {
                    nextDeparture = Double.MAX_VALUE;
                }
                lastEventTime = currentTime;
            }
        }

        while (!queue.isEmpty()) {
            currentTime = nextDeparture;

            queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);
            if (queue.isEmpty()) {
                emptyQueueTime += (currentTime - lastEventTime);
            }

            Customer served = queue.peek();
            if (served.served) {
                queue.poll();
                completedCustomers.add(served);
                busyTime += served.serviceTime;
            }

            if (!queue.isEmpty()) {
                Customer nextCustomer = queue.peek();
                if (currentTime - nextCustomer.arrivalTime <= MAX_WAIT_TIME) {
                    nextCustomer.serviceStartTime = currentTime;
                    nextCustomer.serviceTime = getExponential(mu);
                    nextCustomer.departureTime = currentTime + nextCustomer.serviceTime;
                    nextCustomer.served = true;
                    nextDeparture = nextCustomer.departureTime;
                } else {
                    queue.poll();
                    customersLost++;
                    nextDeparture = queue.isEmpty() ? Double.MAX_VALUE : currentTime;
                }
            } else {
                nextDeparture = Double.MAX_VALUE;
            }
            lastEventTime = currentTime;
        }

        double totalSimulationTime = currentTime;
        SimulationResults results = new SimulationResults();
        double totalWaitingTime = 0.0;
        double totalSystemTime = 0.0;
        for (Customer c : completedCustomers) {
            totalWaitingTime += (c.serviceStartTime - c.arrivalTime);
            totalSystemTime += (c.departureTime - c.arrivalTime);
        }

        results.avgWaitingTime = completedCustomers.isEmpty() ? 0 : totalWaitingTime / completedCustomers.size();
        results.avgSystemTime = completedCustomers.isEmpty() ? 0 : totalSystemTime / completedCustomers.size();
        results.utilizationFactor = busyTime / totalSimulationTime;
        results.idleTimeFraction = 1.0 - results.utilizationFactor;
        results.avgQueueLength = queueLengthTimeProduct / totalSimulationTime;
        results.maxQueueLength = maxQueueLength;
        results.emptyQueueProbability = emptyQueueTime / totalSimulationTime;
        results.customersLost = customersLost;

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
            avgResults.customersLost += results.customersLost;
            validSimulations++;
        }

        avgResults.avgWaitingTime /= validSimulations;
        avgResults.avgSystemTime /= validSimulations;
        avgResults.utilizationFactor /= validSimulations;
        avgResults.idleTimeFraction /= validSimulations;
        avgResults.avgQueueLength /= validSimulations;
        avgResults.emptyQueueProbability /= validSimulations;
        avgResults.customersLost /= validSimulations;

        return avgResults;
    }

    public static void runSimulationAnalysis(double lambda, double mu) {
        System.out.println("\nCoffee Shop Simulation Results (Averaged over " + SIMULATIONS + " runs):");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-25s %-15s %-15s %-15s %-15s %-15s %-15s %-15s\n",
                "Metric", "Avg Wait Time", "Avg Sys Time", "Utilization", "Idle Fraction", "Avg Queue Len", "Max Queue Len", "P(Empty Queue)", "Cust Lost");

        Q4 simulation = new Q4(lambda, mu);
        SimulationResults results = simulation.runMultipleSimulations();

        System.out.printf("%-25s %-15.6f %-15.6f %-15.6f %-15.6f %-15.6f %-15d %-15.6f %-15d\n",
                "Simulation Results",
                results.avgWaitingTime,
                results.avgSystemTime,
                results.utilizationFactor,
                results.idleTimeFraction,
                results.avgQueueLength,
                results.maxQueueLength,
                results.emptyQueueProbability,
                results.customersLost);

        // double rho = lambda / mu;
        // double Lq = (lambda * lambda) / (mu * (mu - lambda));
        // double Wq = Lq / lambda;
        // double W = Wq + (1.0 / mu);
        // double P0 = 1.0 - rho;

        // System.out.println("\nTheoretical Values (for comparison):");
        // System.out.println("--------------------------------------------------");
        // System.out.printf("%-25s %-15.6f %-15.6f %-15.6f %-15.6f %-15.6f %-15s %-15.6f %-15s\n",
        //         "Theoretical Values",
        //         Wq,
        //         W,
        //         rho,
        //         1.0 - rho,
        //         Lq,
        //         "N/A",
        //         P0,
        //         "N/A");
    }

    public static void main(String[] args) {
        double lambda = 10.0; // customers per hour
        double mu = 15.0;     // customers per hour
        runSimulationAnalysis(lambda, mu);
    }
}