#include <iostream>
#include <queue>
#include <vector>
#include <random>
#include <iomanip>

using namespace std;

const double SIMULATION_TIME = 1000.0; // simulation duration in hours
const int SIMULATIONS = 1000;          // number of simulation runs

// Structure for storing customer details
struct Customer {
    double arrivalTime;      // time when customer arrives
    double serviceStartTime; // time when service begins
    double serviceTime;      // duration of service
    double departureTime;    // time when customer leaves

    // Constructor
    explicit Customer(double arrTime) : arrivalTime(arrTime),
        serviceStartTime(0.0), serviceTime(0.0), departureTime(0.0) {}
};

// Structure for storing simulation results
struct SimulationResults {
    double avgWaitingTime;   // average time spent waiting in queue
    double avgSystemTime;    // average total time spent in system
    double utilizationRate;  // proportion of time server is busy
    double avgQueueLength;   // average number of customers in queue
    double probSystemFull;   // probability system is at capacity
    double probRejection;    // probability of customer rejection

    // Default constructor
    SimulationResults() : avgWaitingTime(0.0), avgSystemTime(0.0),
        utilizationRate(0.0), avgQueueLength(0.0),
        probSystemFull(0.0), probRejection(0.0) {}
};

class Q2 {
private:
    mt19937 gen;           // Mersenne Twister random number generator
    double lambda;         // arrival rate (customers per hour)
    double mu;            // service rate (customers per hour)
    int capacity;         // maximum system capacity

    // Generates exponentially distributed random variables
    double getExponential(double rate) {
        uniform_real_distribution<> dis(0.0, 1.0);
        return -log(1.0 - dis(gen)) / rate;
    }

public:
    // Constructor to initialize simulation parameters
    Q2(double l, double m, int cap) : lambda(l), mu(m), capacity(cap) {
        random_device rd;
        gen.seed(rd());
    }

    // Runs a single simulation instance
    SimulationResults runSimulation() {
        queue<Customer> q;              // queue for waiting customers
        vector<Customer> completedCustomers; // completed customers
        
        double currentTime = 0.0;       // current simulation time
        double nextArrival = getExponential(lambda); // time of next arrival
        double nextDeparture = numeric_limits<double>::max(); // time of next departure

        int rejectedCustomers = 0;      // count of rejected customers
        int totalArrivals = 0;          // total number of arrivals
        double busyTime = 0.0;          // total time server is busy
        double queueLengthTimeProduct = 0.0; // for calculating average queue length
        double fullSystemTime = 0.0;    // time system is at capacity
        double lastEventTime = 0.0;     // time of last event

        // Main simulation loop
        while (currentTime < SIMULATION_TIME) {
            // Handle arrival event
            if (nextArrival < nextDeparture) {
                currentTime = nextArrival;
                totalArrivals++;

                // Update queue length time product
                queueLengthTimeProduct += q.size() * (currentTime - lastEventTime);
                if (q.size() == capacity) {
                    fullSystemTime += currentTime - lastEventTime;
                }

                // Process new customer if there's space
                if (q.size() < capacity) {
                    Customer customer(currentTime);
                    q.push(customer);

                    // Start service if queue was empty
                    if (q.size() == 1) {
                        Customer& c = q.front();
                        c.serviceStartTime = currentTime;
                        c.serviceTime = getExponential(mu);
                        c.departureTime = currentTime + c.serviceTime;
                        nextDeparture = c.departureTime;
                    }
                }
                else {
                    rejectedCustomers++; // reject if system is full
                }
                nextArrival = currentTime + getExponential(lambda);
                lastEventTime = currentTime;
            }
            // Handle departure event
            else {
                currentTime = nextDeparture;

                // Update queue length time product
                queueLengthTimeProduct += q.size() * (currentTime - lastEventTime);
                if (q.size() == capacity) {
                    fullSystemTime += currentTime - lastEventTime;
                }

                Customer served = q.front();
                q.pop();
                completedCustomers.push_back(served);
                busyTime += served.serviceTime;

                // Serve next customer if queue isn't empty
                if (!q.empty()) {
                    Customer& nextCustomer = q.front();
                    nextCustomer.serviceStartTime = currentTime;
                    nextCustomer.serviceTime = getExponential(mu);
                    nextCustomer.departureTime = currentTime + nextCustomer.serviceTime;
                    nextDeparture = nextCustomer.departureTime;
                }
                else {
                    nextDeparture = numeric_limits<double>::max();
                }
                lastEventTime = currentTime;
            }
        }

        // Calculate performance measures
        SimulationResults results;
        double totalWaitingTime = 0.0;
        double totalSystemTime = 0.0;
        for (const Customer& c : completedCustomers) {
            totalWaitingTime += c.serviceStartTime - c.arrivalTime;
            totalSystemTime += c.departureTime - c.arrivalTime;
        }
        results.avgWaitingTime = totalWaitingTime / completedCustomers.size();
        results.avgSystemTime = totalSystemTime / completedCustomers.size();
        results.utilizationRate = busyTime / currentTime;
        results.avgQueueLength = queueLengthTimeProduct / currentTime;
        results.probSystemFull = fullSystemTime / currentTime;
        results.probRejection = static_cast<double>(rejectedCustomers) / totalArrivals;

        return results;
    }

    // Runs multiple simulations and averages results
    SimulationResults runMultipleSimulations() {
        SimulationResults avgResults;
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

        // Calculate averages
        avgResults.avgWaitingTime /= validSimulations;
        avgResults.avgSystemTime /= validSimulations;
        avgResults.utilizationRate /= validSimulations;
        avgResults.avgQueueLength /= validSimulations;
        avgResults.probSystemFull /= validSimulations;
        avgResults.probRejection /= validSimulations;

        return avgResults;
    }
};

// Analyzes effect of different capacities on system performance
void analyzeCapacityEffect(double lambda, double mu, int minCapacity, int maxCapacity) {
    cout << "\nCapacity Analysis Results:" << endl;
    cout << "--------------------------------------------------" << endl;
    // Print header
    cout << setw(10) << "Capacity" << setw(15) << "Avg Wait Time" << setw(15) << "Avg Sys Time"
         << setw(15) << "Utilization" << setw(15) << "Avg Queue Len" << setw(15) << "P(System Full)"
         << setw(15) << "P(Rejection)" << endl;

    // Run simulations for each capacity
    for (int capacity = minCapacity; capacity <= maxCapacity; capacity++) {
        Q2 simulation(lambda, mu, capacity);
        SimulationResults results = simulation.runMultipleSimulations();

        // Print formatted results
        cout << fixed << setprecision(6)
             << setw(10) << capacity
             << setw(15) << results.avgWaitingTime
             << setw(15) << results.avgSystemTime
             << setw(15) << results.utilizationRate
             << setw(15) << results.avgQueueLength
             << setw(15) << results.probSystemFull
             << setw(15) << results.probRejection << endl;
    }
}

// Main function to run the analysis
int main() {
    double lambda = 20.0; // arrival rate
    double mu = 24.0;     // service rate

    analyzeCapacityEffect(lambda, mu, 3, 7); // analyze capacities 3 through 7

    return 0;
}