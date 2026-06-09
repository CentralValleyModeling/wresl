import matplotlib.pyplot as plt
import pulp
from optiplot import plot_problem
from optiplot.logs import get_logger

LOGGER = get_logger(__name__)


def tutorials_index_01():
    problem: pulp.LpProblem = pulp.LpProblem("tutorials_index_01", pulp.LpMaximize)
    # Constraints
    OUTFLOW = problem.add_variable("OUTFLOW", 0, None)
    DELIVERY = problem.add_variable("DELIVERY", 0, 50)
    # Objective
    problem += (10 * DELIVERY) + (1 * OUTFLOW)
    # Constraints
    problem += OUTFLOW >= (0.25 * DELIVERY) + 25, "MINIMUM_FLOW_REQUIREMENT"
    problem += 60 - OUTFLOW - DELIVERY == 0, "MASS_BALANCE"
    # Solve so we can plot the optimal point
    LOGGER.info("solving pulp problem".center(50, "."))
    problem.solve(solver=pulp.getSolver("PULP_CBC_CMD"))
    LOGGER.info("pulp problem done solving".center(50, "."))

    # Plot things
    plot_problem("DELIVERY", "OUTFLOW", problem=problem)
    plt.show()


def tutorials_index_02():
    problem: pulp.LpProblem = pulp.LpProblem("tutorials_index_02", pulp.LpMaximize)
    # Constraints
    OUTFLOW = problem.add_variable("OUTFLOW", 0, None)
    DELIVERY = problem.add_variable("DELIVERY", 0, 50)
    UNMET_DEMAND = problem.add_variable("UNMET_DEMAND", 0, None)
    demand = 100
    # Objective
    problem += (10 * DELIVERY) + (1 * OUTFLOW) + (-1000 * UNMET_DEMAND)

    # Constraints
    problem += OUTFLOW >= (0.25 * DELIVERY) + 25, "MINIMUM_FLOW_REQUIREMENT"
    problem += 60 - OUTFLOW - DELIVERY == 0, "MASS_BALANCE"
    problem += demand - DELIVERY - UNMET_DEMAND == 0, "DEMAND"

    # Solve so we can plot the optimal point
    LOGGER.info("solving pulp problem".center(50, "."))
    problem.solve(solver=pulp.getSolver("PULP_CBC_CMD"))
    LOGGER.info("pulp problem done solving".center(50, "."))

    # Plot things
    plot_problem("DELIVERY", "UNMET_DEMAND", problem=problem)
    plt.show()


if __name__ == "__main__":
    tutorials_index_02()
