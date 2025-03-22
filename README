# Queue Simulation Project

This repository contains three Java simulation programs that model different queueing systems using discrete-event simulation techniques. Each simulation explores various operational scenarios and collects performance metrics to help analyze system behavior under different conditions.

## Overview of Simulations

1. **Q1: Queue Simulation with Changing Servers**  
   Models a multi-server queue where the number of active servers changes during the simulation. This program collects performance metrics for three time periods as well as for the entire simulation duration.

2. **Q2: Capacity Analysis Simulation**  
   Simulates a queueing system with finite capacity. In this model, arriving customers are rejected if the system is full. The simulation is run repeatedly (over 1000 simulation runs) for different capacity values, allowing a capacity-effect analysis.

3. **Q3: Coffee Shop Simulation**  
   Simulates a single-server queue (representing a coffee shop) where exactly 500 customer arrivals are processed per run. The simulation runs for 1000 independent runs to average performance metrics, and it compares simulated results with theoretical values.



## Detailed Descriptions

### Q1: Queue Simulation with Changing Servers

**Objective:**  
Analyze how dynamic server allocation impacts system performance.

**Key Features:**
  **Dynamic Server Allocation:**  
    **0-2 hours:** 2 servers  
    **2-5 hours:** 4 servers  
    **5-8 hours:** 3 servers  
  **Metrics Collected:**  
  - Average waiting time  
  - Average system time (waiting time + service time)  
  - Server utilization  
  - Average queue length (time-weighted)  
  - Probability that all active servers are busy

**How It Works:**  
- The simulation continuously processes events (arrivals and departures).
- Customers are served immediately if a server is free; otherwise, they wait in an infinite queue.
- When the number of active servers changes, any customer being served by a deactivated server is moved back to the queue.

**Compilation & Execution:**
```bash
javac Q1.java
java Q1
```

---

### Q2: Capacity Analysis Simulation

**Objective:**  
Examine the impact of a finite system capacity on queue performance and customer rejection.

**Key Features:**
- **Finite Capacity:**  
  Customers are rejected if they arrive when the queue is at capacity.
 **Multiple Simulation Runs:**  
  Each simulation run represents 1000 hours, and 1000 runs are averaged to obtain robust metrics.
  **Metrics Collected:**  
  - Average waiting time  
  - Average system time  
  - Utilization rate  
  - Average queue length  
  - Probability the system is full  
  - Probability of customer rejection

**How It Works:**  
- Customers arrive according to an exponential distribution.
- If the queue is not full, a customer is admitted; otherwise, they are rejected.
- The simulation aggregates results over a range of capacity values (e.g., capacities 3 to 7).

**Compilation & Execution:**
```bash
javac Q2.java
java Q2
```

---

### Q3: Coffee Shop Simulation

**Objective:**  
Simulate a single-server queue (akin to a coffee shop) to analyze performance metrics over a fixed number of customer arrivals and compare them with theoretical values.

**Key Features:**
- **Fixed Number of Arrivals:**  
  Each simulation run processes exactly 500 customer arrivals.
- **Multiple Simulation Runs:**  
  The simulation is repeated 1000 times to average out the performance metrics.
- **Metrics Collected:**  
  - Average waiting time  
  - Average system time  
  - Utilization factor (fraction of busy time)  
  - Idle time fraction  
  - Average queue length  
  - Maximum queue length reached  
  - Probability that the queue is empty
- **Theoretical Comparison:**  
  The simulation calculates theoretical performance values for the M/M/1 queue (using formulas such as ρ = λ/μ, Lq, Wq, etc.) for comparison with the simulation results.

**How It Works:**  
- Customer interarrival and service times are generated using an exponential distribution.
- The simulation continues until 500 arrivals have been processed, after which any remaining customers are served.
- Metrics are computed over each run and then averaged across 1000 runs.

**Compilation & Execution:**
```bash
javac Q3.java
java Q3
```

---

## Requirements

- **Java Development Kit (JDK):** Version 8 or higher is required to compile and run these programs.

---

## Customization

You can modify simulation parameters such as arrival rates (λ), service rates (μ), server capacities, and the number of simulation runs directly in the source code files. These parameters are specified in the `main` methods or as constants at the top of each file.



## Understanding the Output

### Q1 Output:
  **Period-Specific Metrics:**  
  Detailed tables for each time period (0-2, 2-5, 5-8 hours) that include average wait time, system time, utilization, average queue length, and probability that all servers are busy.
  **Entire Day Metrics:**  
  Aggregated results for the full simulation period.

### Q2 Output:
- **Capacity Analysis Table:**  
  A table displaying performance metrics for each capacity level tested (from a minimum to maximum value). Metrics include average waiting and system times, utilization, average queue length, probability that the system is full, and probability of rejection.

### Q3 Output:
- **Coffee Shop Simulation Results:**  
  A summary of the averaged metrics over 1000 simulation runs including waiting time, system time, utilization, idle fraction, average and maximum queue lengths, and empty queue probability.
- **Theoretical Values:**  
  A comparison of the simulated results against the theoretical values derived for an M/M/1 queue.



## Conclusion

This project offers three different simulation models to study various queueing phenomena:
- **Q1** provides insight into systems with dynamic resource allocation.
- **Q2** investigates the effects of finite capacity on service performance and customer loss.
- **Q3** models a practical single-server scenario (such as a coffee shop) and compares simulation outcomes with theoretical predictions.

Feel free to experiment with the parameters to observe how different configurations impact system performance. Enjoy exploring and analyzing these queueing models!

