import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Q2 {
    private static final double SIMULATION_TIME = 1000.0; // hours
    private static final int SIMULATIONS = 1000; // number of simulation runs

    // class for storing the details of jobs arriving at the server
    static class Customer
    {
        double arrivalTime;
        double serviceStartTime;
        double serviceTime;
        double departureTime;

        Customer(double arrivalTime) {
            this.arrivalTime = arrivalTime;
        }
    }

    // class for storing the results
    static class SimulationResults
    {
        double avgWaitingTime;
        double avgSystemTime;
        double utilizationRate;
        double avgQueueLength;
        double probSystemFull;
        double probRejection;
    }

    private Random random;
    private double lambda; // arrival rate
    private double mu;    // service rate
    private int capacity;  // system capacity
    // cunstructor to assign the details
    public  Q2(double lambda, double mu, int capacity) {
        
        this.random = new Random();
        this.lambda = lambda;
        this.mu = mu;
        this.capacity = capacity;
        
    }
    // generates exponential distribution
    private double getExponential(double rate)
    {
        return -Math.log(1.0 - random.nextDouble())/rate;
    }

    private SimulationResults runSimulation()
    {
        Queue<Customer> queue = new LinkedList<>();
        ArrayList<Customer> completedCustomers = new ArrayList<>();
        
        double currentTime = 0.0;
        double nextArrival = getExponential(lambda);
        double nextDeparture = Double.MAX_VALUE;

        int rejectedCustomers = 0;
        int totalArrivals = 0;
        double busyTime = 0.0;
        double queueLengthTimeProduct = 0.0;
        double fullSystemTime = 0.0;
        double lastEventTime = 0.0;

        while (currentTime < SIMULATION_TIME)
        {
            // Handle arrival
            if (nextArrival < nextDeparture)
            {
                currentTime = nextArrival;
                totalArrivals++;

                // Update queue length time product
                queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);
                if( queue.size() == capacity)
                {
                    fullSystemTime += currentTime - lastEventTime;
                }

                // Create new customer
                if (queue.size() < capacity)
                {
                    Customer customer = new Customer(currentTime);
                    queue.add(customer);

                    // If this is the only customer, start service
                    if (queue.size() == 1)
                    {
                        customer.serviceStartTime = currentTime;
                        customer.serviceTime = getExponential(mu);
                        customer.departureTime = currentTime + customer.serviceTime;
                        nextDeparture = customer.departureTime;
                    }
                }
                else
                {
                    rejectedCustomers++;
                }
                nextArrival = currentTime + getExponential(lambda);
                lastEventTime = currentTime;
            }
            // Handle departure
            else
            {
                currentTime  = nextDeparture;

                // Update queue length time product
                queueLengthTimeProduct += queue.size() * (currentTime - lastEventTime);
                if( queue.size() == capacity)
                {
                    fullSystemTime += currentTime - lastEventTime;
                }

                Customer served = queue.poll();
                completedCustomers.add(served);
                busyTime += served.serviceTime;

                // If there are more customers, start serving next
                if ( !queue.isEmpty())
                {
                    Customer nexCustomer = queue.peek();
                    nexCustomer.serviceStartTime = currentTime;
                    nexCustomer.serviceTime = getExponential(mu);
                    nexCustomer.departureTime = currentTime + nexCustomer.serviceTime;
                    nextDeparture = nexCustomer.departureTime;
                }
                else
                {
                    nextDeparture = Double.MAX_VALUE;
                }
                lastEventTime = currentTime;
            }
        }

        // Calculate the performance measures
        SimulationResults results = new SimulationResults();
        double totalWaitingTime = 0.0;
        double totalSystemTime = 0.0;
        for (Customer c : completedCustomers)
        {
            totalWaitingTime += c.serviceStartTime - c.arrivalTime;
            totalSystemTime += c.departureTime - c.arrivalTime;
        }
        results.avgWaitingTime = totalWaitingTime / completedCustomers.size();
        results.avgSystemTime = totalSystemTime / completedCustomers.size();
        results.utilizationRate = busyTime / currentTime;
        results.avgQueueLength = queueLengthTimeProduct / currentTime;
        results.probSystemFull = fullSystemTime / currentTime;
        results.probRejection = (double) rejectedCustomers / totalArrivals;
        //System.out.println(totalArrivals);
        return results;
    }

    private SimulationResults runMultipleSimulations() {
        SimulationResults avgResults = new SimulationResults();
        int validSimulations = 0;

        for (int i = 0; i < SIMULATIONS; i++) {
            SimulationResults results = runSimulation();
            avgResults.avgWaitingTime += results.avgWaitingTime;
            avgResults.avgSystemTime += results.avgSystemTime;
            avgResults.utilizationRate += results.utilizationRate;
            avgResults.avgQueueLength += results.avgQueueLength;
            avgResults.probSystemFull += results.probSystemFull;
            avgResults.probRejection += results.probRejection;
            validSimulations++;
        }

        // Average the results
        avgResults.avgWaitingTime /= validSimulations;
        avgResults.avgSystemTime /= validSimulations;
        avgResults.utilizationRate /= validSimulations;
        avgResults.avgQueueLength /= validSimulations;
        avgResults.probSystemFull /= validSimulations;
        avgResults.probRejection /= validSimulations;

        return avgResults;
    }

    public static void analyzeCapacityEffect(double lambda, double mu, int minCapacity, int maxCapacity)
    {
        System.out.println("\nCapacity Analysis Results:");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-10s %-15s %-15s %-15s %-15s %-15s %-15s%n",
                "Capacity", "Avg Wait Time", "Avg Sys Time", "Utilization", "Avg Queue Len", "P(System Full)", "P(Rejection)");
        for (int capacity = minCapacity; capacity <= maxCapacity; capacity++)
        {
            Q2 simulation = new Q2(lambda, mu, capacity);
            SimulationResults results = simulation.runMultipleSimulations();

            System.out.printf("%-10d %-15.6f %-15.6f %-15.6f %-15.6f %-15.6f %-15.6f%n",
                    capacity,
                    results.avgWaitingTime,
                    results.avgSystemTime,
                    results.utilizationRate,
                    results.avgQueueLength,
                    results.probSystemFull,
                    results.probRejection);
        }
    }



    public static void main(String[] args)
    {
        double lambda = 20.0;
        double mu = 24.0;

        analyzeCapacityEffect(lambda, mu, 3, 7);

    }
}
