#include <iostream>
#include <queue>
#include <vector>
#include <random>
#include <iomanip>

using namespace std;

const double SIMULATION_TIME = 8.0; // simulation duration in hours

// Structure for customer details
struct Customer {
    double arrivalTime;      // time of arrival
    double serviceStartTime; // time service begins
    double serviceTime;      // duration of service
    double departureTime;    // time of departure
    int serverId;            // ID of server handling this customer

    // Constructor
    explicit Customer(double arrTime) : arrivalTime(arrTime),
        serviceStartTime(0.0), serviceTime(0.0), departureTime(0.0), serverId(-1) {}
};

// Structure for server state
struct Server {
    bool isBusy;            // whether server is currently busy
    Customer* currentCustomer; // pointer to customer being served
    double busyUntil;       // time when server will be free

    // Constructor
    Server() : isBusy(false), currentCustomer(nullptr), busyUntil(0.0) {}
};

// Structure for storing results of a period
struct PeriodResults {
    double totalWaitingTime;     // total waiting time of all customers
    double totalSystemTime;      // total system time of all customers
    double totalBusyTime;        // total time servers were busy
    double queueLengthTimeProduct; // for calculating average queue length
    double allBusyTime;          // time when all servers were busy
    int completedCustomers;      // number of customers served
    double periodDuration;       // duration of this period

    // Constructor
    PeriodResults(double duration) : totalWaitingTime(0.0), totalSystemTime(0.0),
        totalBusyTime(0.0), queueLengthTimeProduct(0.0), allBusyTime(0.0),
        completedCustomers(0), periodDuration(duration) {}

    // Add a completed customer's metrics
    void addCustomer(const Customer& c) {
        totalWaitingTime += c.serviceStartTime - c.arrivalTime;
        totalSystemTime += c.departureTime - c.arrivalTime;
        completedCustomers++;
    }

    // Add server busy time
    void addBusyTime(double time) {
        totalBusyTime += time;
    }

    // Update queue length metric
    void addQueueLengthTime(double queueLength, double time) {
        queueLengthTimeProduct += queueLength * time;
    }

    // Add time when all servers were busy
    void addAllBusyTime(double time) {
        allBusyTime += time;
    }

    // Calculate average waiting time
    double getAvgWaitingTime() {
        return completedCustomers > 0 ? totalWaitingTime / completedCustomers : 0.0;
    }

    // Calculate average system time
    double getAvgSystemTime() {
        return completedCustomers > 0 ? totalSystemTime / completedCustomers : 0.0;
    }

    // Calculate utilization rate (total busy time / total server-hours)
    double getUtilizationRate(int servers) {
        return totalBusyTime / (periodDuration * servers);
    }

    // Calculate average queue length
    double getAvgQueueLength() {
        return queueLengthTimeProduct / periodDuration;
    }

    // Calculate probability all servers are busy
    double getProbAllBusy() {
        return allBusyTime / periodDuration;
    }
};

class Q2 {
private:
    mt19937 gen;        // Mersenne Twister random number generator
    double lambda;      // arrival rate (customers per hour)
    double mu;          // service rate per server (customers per hour)
    int maxServers;     // maximum number of servers

    // Generates exponentially distributed random variables
    double getExponential(double rate) {
        uniform_real_distribution<> dis(0.0, 1.0);
        return -log(1.0 - dis(gen)) / rate;
    }

    // Returns number of active servers based on current time
    int getActiveServers(double currentTime) {
        if (currentTime < 2.0) return 2;      // First 2 hours: 2 servers
        else if (currentTime < 5.0) return 4; // Next 3 hours: 4 servers
        else return 3;                        // Last 3 hours: 3 servers
    }

    // Returns period index based on time (0: 0-2h, 1: 2-5h, 2: 5-8h)
    int getPeriod(double time) {
        if (time < 2.0) return 0;
        else if (time < 5.0) return 1;
        else return 2;
    }

    // Finds an available server among active servers
    int findAvailableServer(vector<Server>& servers, int activeServers) {
        for (int i = 0; i < activeServers; i++) {
            if (!servers[i].isBusy) return i;
        }
        return -1;
    }

    // Counts busy servers among active servers
    int countBusyServers(vector<Server>& servers, int activeServers) {
        int busyCount = 0;
        for (int i = 0; i < activeServers; i++) {
            if (servers[i].isBusy) busyCount++;
        }
        return busyCount;
    }

public:
    // Constructor
    Q2(double l, double m) : lambda(l), mu(m), maxServers(4) {
        random_device rd;
        gen.seed(rd());
    }

    // Runs the simulation
    void runSimulation() {
        queue<Customer> q;             // queue for waiting customers
        vector<Customer> completedCustomers; // completed customers
        vector<Server> servers(maxServers); // array of servers

        // Initialize results for three periods and entire day
        vector<PeriodResults> periodResults = {
            PeriodResults(2.0), // 0-2 hours
            PeriodResults(3.0), // 2-5 hours
            PeriodResults(3.0)  // 5-8 hours
        };
        PeriodResults entireDayResults(SIMULATION_TIME);

        double currentTime = 0.0;      // current simulation time
        double nextArrival = getExponential(lambda); // time of next arrival
        double lastEventTime = currentTime;
        int totalArrivals = 0;

        // Main simulation loop
        while (currentTime < SIMULATION_TIME) {
            int activeServers = getActiveServers(currentTime);
            int currentPeriod = getPeriod(currentTime);

            // Find earliest departure among active servers
            double nextDeparture = numeric_limits<double>::max();
            int departingServer = -1;
            for (int i = 0; i < activeServers; i++) {
                if (servers[i].isBusy && servers[i].busyUntil < nextDeparture) {
                    nextDeparture = servers[i].busyUntil;
                    departingServer = i;
                }
            }

            // Determine next event
            double nextEventTime = min(nextArrival, nextDeparture);
            bool isArrivalEvent = (nextArrival < nextDeparture);

            // Update metrics for time interval
            double timeInterval = nextEventTime - currentTime;
            if (timeInterval > 0) {
                periodResults[currentPeriod].addQueueLengthTime(q.size(), timeInterval);
                entireDayResults.addQueueLengthTime(q.size(), timeInterval);

                int busyServers = countBusyServers(servers, activeServers);
                if (busyServers == activeServers) {
                    periodResults[currentPeriod].addAllBusyTime(timeInterval);
                    entireDayResults.addAllBusyTime(timeInterval);
                }
            }

            // Advance time
            currentTime = nextEventTime;
            lastEventTime = currentTime;

            if (isArrivalEvent) {
                // Handle arrival
                totalArrivals++;
                Customer customer(currentTime);
                int availableServer = findAvailableServer(servers, activeServers);
                if (availableServer != -1) {
                    // Assign to available server
                    customer.serviceStartTime = currentTime;
                    customer.serviceTime = getExponential(mu);
                    customer.departureTime = currentTime + customer.serviceTime;
                    customer.serverId = availableServer;

                    servers[availableServer].isBusy = true;
                    servers[availableServer].currentCustomer = new Customer(customer);
                    servers[availableServer].busyUntil = customer.departureTime;
                }
                else {
                    // Add to queue if no server available
                    q.push(customer);
                }
                nextArrival = currentTime + getExponential(lambda);
            }
            else {
                // Handle departure
                Customer served = *servers[departingServer].currentCustomer;
                completedCustomers.push_back(served);
                int departurePeriod = getPeriod(served.departureTime);
                periodResults[departurePeriod].addCustomer(served);
                entireDayResults.addCustomer(served);
                periodResults[departurePeriod].addBusyTime(served.serviceTime);
                entireDayResults.addBusyTime(served.serviceTime);

                // Free the server
                delete servers[departingServer].currentCustomer;
                servers[departingServer].isBusy = false;
                servers[departingServer].currentCustomer = nullptr;

                // Serve next customer if queue not empty
                if (!q.empty()) {
                    Customer nextCustomer = q.front();
                    q.pop();
                    int availableServer = findAvailableServer(servers, activeServers);
                    if (availableServer != -1) {
                        nextCustomer.serviceStartTime = currentTime;
                        nextCustomer.serviceTime = getExponential(mu);
                        nextCustomer.departureTime = currentTime + nextCustomer.serviceTime;
                        nextCustomer.serverId = availableServer;

                        servers[availableServer].isBusy = true;
                        servers[availableServer].currentCustomer = new Customer(nextCustomer);
                        servers[availableServer].busyUntil = nextCustomer.departureTime;
                    }
                    else {
                        q.push(nextCustomer); // Shouldn't happen right after departure
                    }
                }
            }

            // Handle server reduction
            for (int i = activeServers; i < maxServers; i++) {
                if (servers[i].isBusy) {
                    Customer interrupted = *servers[i].currentCustomer;
                    delete servers[i].currentCustomer;
                    servers[i].isBusy = false;
                    servers[i].currentCustomer = nullptr;
                    q.push(interrupted);
                }
            }
        }

        // Print results
        cout << "\nSimulation Results (with changing servers and infinite queue):" << endl;
        cout << "--------------------------------------------------" << endl;
        cout << setw(20) << left << "Period" << setw(15) << "Avg Wait Time"
             << setw(15) << "Avg Sys Time" << setw(15) << "Utilization"
             << setw(15) << "Avg Queue Len" << setw(15) << "P(All Busy)" << endl;

        // Results for each period
        for (int i = 0; i < 3; i++) {
            string periodLabel;
            int serverCount;
            if (i == 0) { periodLabel = "0-2 hours"; serverCount = 2; }
            else if (i == 1) { periodLabel = "2-5 hours"; serverCount = 4; }
            else { periodLabel = "5-8 hours"; serverCount = 3; }

            cout << fixed << setprecision(6)
                 << setw(20) << left << periodLabel
                 << setw(15) << periodResults[i].getAvgWaitingTime()
                 << setw(15) << periodResults[i].getAvgSystemTime()
                 << setw(15) << periodResults[i].getUtilizationRate(serverCount)
                 << setw(15) << periodResults[i].getAvgQueueLength()
                 << setw(15) << periodResults[i].getProbAllBusy() << endl;
        }

        // Entire day results
        cout << "\nEntire Day Results:" << endl;
        cout << setw(20) << left << "Period" << setw(15) << "Avg Wait Time"
             << setw(15) << "Avg Sys Time" << setw(15) << "Utilization"
             << setw(15) << "Avg Queue Len" << setw(15) << "P(All Busy)" << endl;
        double entireDayUtilization = entireDayResults.totalBusyTime / 25.0; // 25 server-hours total
        cout << fixed << setprecision(6)
             << setw(20) << left << "0-8 hours"
             << setw(15) << entireDayResults.getAvgWaitingTime()
             << setw(15) << entireDayResults.getAvgSystemTime()
             << setw(15) << entireDayUtilization
             << setw(15) << entireDayResults.getAvgQueueLength()
             << setw(15) << entireDayResults.getProbAllBusy() << endl;
    }

    // Cleanup not needed in this scope as vectors handle memory
};

// Analyzes server effect with given parameters
void analyzeServerEffect(double lambda, double mu) {
    Q2 simulation(lambda, mu);
    simulation.runSimulation();
}

// Main function
int main() {
    double lambda = 40.0; // arrival rate
    double mu = 15.0;     // service rate per server
    
    analyzeServerEffect(lambda, mu);
    
    return 0;
}