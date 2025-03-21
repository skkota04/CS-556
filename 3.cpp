#include <iostream>
#include <queue>
#include <vector>
#include <random>
#include <iomanip>

using namespace std;

const int ARRIVALS = 500;    // number of customer arrivals to simulate
const int SIMULATIONS = 1; // number of simulation runs

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
    double avgWaitingTime;    // average time spent waiting in queue
    double avgSystemTime;     // average total time in system
    double utilizationFactor; // proportion of time server is busy
    double idleTimeFraction;  // proportion of time server is idle
    double avgQueueLength;    // average number of customers in queue
    int maxQueueLength;       // maximum queue length observed
    double emptyQueueProbability; // probability queue is empty

    // Default constructor
    SimulationResults() : avgWaitingTime(0.0), avgSystemTime(0.0),
        utilizationFactor(0.0), idleTimeFraction(0.0),
        avgQueueLength(0.0), maxQueueLength(0), emptyQueueProbability(0.0) {}
};

class Q3 {
private:
    mt19937 gen;     // Mersenne Twister random number generator
    double lambda;   // arrival rate (customers per hour)
    double mu;       // service rate (customers per hour)

    // Generates exponentially distributed random variables
    double getExponential(double rate) {
        uniform_real_distribution<> dis(0.0, 1.0);
        return -log(1.0 - dis(gen)) / rate;
    }

public:
    // Constructor to initialize simulation parameters
    Q3(double l, double m) : lambda(l), mu(m) {
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
        double busyTime = 0.0;          // total time server is busy
        double queueLengthTimeProduct = 0.0; // for calculating average queue length
        double emptyQueueTime = 0.0;    // total time queue is empty
        double lastEventTime = 0.0;     // time of last event
        int totalArrivals = 0;          // total number of arrivals
        int maxQueueLength = 0;         // maximum observed queue length

        // Process until specified number of arrivals
        while (totalArrivals < ARRIVALS) {
            // Handle arrival event
            if (nextArrival < nextDeparture) {
                currentTime = nextArrival;
                totalArrivals++;

                // Update queue metrics
                queueLengthTimeProduct += q.size() * (currentTime - lastEventTime);
                if (q.empty()) {
                    emptyQueueTime += (currentTime - lastEventTime);
                }

                Customer customer(currentTime);
                q.push(customer);

                // Update maximum queue length
                maxQueueLength = max(maxQueueLength, static_cast<int>(q.size()));

                // Start service if queue was empty
                if (q.size() == 1) {
                    Customer& c = q.front();
                    c.serviceStartTime = currentTime;
                    c.serviceTime = getExponential(mu);
                    c.departureTime = currentTime + c.serviceTime;
                    nextDeparture = c.departureTime;
                }

                nextArrival = currentTime + getExponential(lambda);
                lastEventTime = currentTime;
            }
            // Handle departure event
            else {
                currentTime = nextDeparture;

                // Update queue metrics
                queueLengthTimeProduct += q.size() * (currentTime - lastEventTime);
                if (q.empty()) {
                    emptyQueueTime += (currentTime - lastEventTime);
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

        // Process remaining customers in queue
        while (!q.empty()) {
            currentTime = nextDeparture;

            // Update queue metrics
            queueLengthTimeProduct += q.size() * (currentTime - lastEventTime);
            if (q.empty()) {
                emptyQueueTime += (currentTime - lastEventTime);
            }

            Customer served = q.front();
            q.pop();
            completedCustomers.push_back(served);
            busyTime += served.serviceTime;

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

        // Total simulation time is when last customer departs
        double totalSimulationTime = currentTime;

        // Calculate performance measures
        SimulationResults results;
        double totalWaitingTime = 0.0;
        double totalSystemTime = 0.0;
        for (const Customer& c : completedCustomers) {
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

    // Runs multiple simulations and averages results
    SimulationResults runMultipleSimulations() {
        SimulationResults avgResults;
        int validSimulations = 0;

        for (int i = 0; i < SIMULATIONS; i++) {
            SimulationResults results = runSimulation();
            avgResults.avgWaitingTime += results.avgWaitingTime;
            avgResults.avgSystemTime += results.avgSystemTime;
            avgResults.utilizationFactor += results.utilizationFactor;
            avgResults.idleTimeFraction += results.idleTimeFraction;
            avgResults.avgQueueLength += results.avgQueueLength;
            avgResults.maxQueueLength = max(avgResults.maxQueueLength, results.maxQueueLength);
            avgResults.emptyQueueProbability += results.emptyQueueProbability;
            validSimulations++;
        }

        // Calculate averages
        avgResults.avgWaitingTime /= validSimulations;
        avgResults.avgSystemTime /= validSimulations;
        avgResults.utilizationFactor /= validSimulations;
        avgResults.idleTimeFraction /= validSimulations;
        avgResults.avgQueueLength /= validSimulations;
        avgResults.emptyQueueProbability /= validSimulations;

        return avgResults;
    }
};

// Runs simulation and compares with theoretical values
void runSimulationAnalysis(double lambda, double mu) {
    cout << "\nCoffee Shop Simulation Results (Averaged over " << SIMULATIONS << " runs):" << endl;
    cout << "--------------------------------------------------" << endl;
    // Print header
    cout << setw(25) << left << "Metric" 
         << setw(15) << "Avg Wait Time" << setw(15) << "Avg Sys Time"
         << setw(15) << "Utilization" << setw(15) << "Idle Fraction"
         << setw(15) << "Avg Queue Len" << setw(15) << "Max Queue Len"
         << setw(15) << "P(Empty Queue)" << endl;

    Q3 simulation(lambda, mu);
    SimulationResults results = simulation.runMultipleSimulations();

    // Print simulation results
    cout << fixed << setprecision(6)
         << setw(25) << left << "Simulation Results"
         << setw(15) << results.avgWaitingTime
         << setw(15) << results.avgSystemTime
         << setw(15) << results.utilizationFactor
         << setw(15) << results.idleTimeFraction
         << setw(15) << results.avgQueueLength
         << setw(15) << results.maxQueueLength
         << setw(15) << results.emptyQueueProbability << endl;

    // Calculate and print theoretical values
    cout << "\nTheoretical Values (for comparison):" << endl;
    cout << "--------------------------------------------------" << endl;
    double rho = lambda / mu;            // utilization factor
    double Lq = (lambda * lambda) / (mu * (mu - lambda)); // average number in queue
    double Wq = Lq / lambda;            // average waiting time in queue
    double W = Wq + (1.0 / mu);         // average time in system
    double P0 = 1.0 - rho;              // probability of empty system

    cout << fixed << setprecision(6)
         << setw(25) << left << "Theoretical Values"
         << setw(15) << Wq
         << setw(15) << W
         << setw(15) << rho
         << setw(15) << 1.0 - rho
         << setw(15) << Lq
         << setw(15) << "N/A"
         << setw(15) << P0 << endl;
}

// Main function to run the analysis
int main() {
    double lambda = 10.0; // arrival rate (customers per hour)
    double mu = 15.0;     // service rate (customers per hour)
    
    runSimulationAnalysis(lambda, mu);
    
    return 0;
}