import matplotlib.pyplot as plt

from matplotlib.axes import Axes
from matplotlib.figure import Figure
import pulp

from .solution import add_optimal_marker
from .constraints import (
    add_constraint_from_slope_intercept,
    add_constraint_at_x,
    add_constraint_at_y,
    calc_interactivity,
)
from .mb import calc_slope_intercept
from .axes_manipulation import get_bounds, make_key
from .style import ConstraintStyle, VariableLimitStyle
from .logs import get_logger

LOGGER = get_logger(__name__)


def plot_problem(*variable_names, problem: pulp.LpProblem) -> tuple[Figure, Axes]:
    LOGGER.info(
        f"plotting {len(variable_names)} variables from pulp problem: {variable_names}"
    )
    if len(variable_names) > 2:
        raise ValueError("Currently, only 1-2 variables are supported")
    elif len(variable_names) == 0:
        raise ValueError("Must provide at least 1 variable")
    variable_dict = {v.name: v for v in problem.variables()}
    variables = [variable_dict[k] for k in variable_names]
    constraints = list()
    for constraint in problem.constraints():
        for var in variables:
            if var in constraint:
                constraints.append(constraint)
                break

    if len(variables) == 2:
        fig, ax = plot_problem_2d(
            variables[0],
            variables[1],
            problem,
            *constraints,
        )
    else:
        raise ValueError("How did we get here?")
    return fig, ax


def plot_problem_2d(
    x: pulp.LpVariable,
    y: pulp.LpVariable,
    problem: pulp.LpProblem,
    *constraints: pulp.LpConstraint,
) -> tuple[Figure, Axes]:
    LOGGER.debug("plotting 2D slice of a problem")
    fig, ax = plt.subplots(figsize=(6, 6))
    # Set bounds of plot
    xlo, xhi = get_bounds(x)
    ylo, yhi = get_bounds(y)
    ax.set_xlim(xlo, xhi)
    ax.set_ylim(ylo, yhi)
    plot_variable_bounds(ax, x, y)
    # Add constraints
    for constraint in constraints:
        if (x not in constraint.expr) and (y not in constraint.expr):
            continue
        LOGGER.info(f"")
        if (constraint.pi is None) or (constraint.pi == 0):
            LOGGER.debug(f"{constraint.name} shadow price is zero")
            # continue
        interactivity = calc_interactivity(constraint, x, y)
        m, b, kind = calc_slope_intercept(x, y, constraint)
        constraint_style = ConstraintStyle(constraint, interactivity)
        if m == 0:
            try:
                _ = constraint.expr[x]  # This will throw if x isn't in expression
                LOGGER.info(f"plotting x limit: {x.name} {kind} {b}")
                add_constraint_at_x(
                    ax,
                    b,
                    kind,
                    constraint_style.line_style_dict,
                    constraint_style.area_style_dict,
                    constraint_style.line_label,
                )
            except Exception:
                LOGGER.info(f"plotting y limit: {y.name} {kind} {b}")
                add_constraint_at_y(
                    ax,
                    b,
                    kind,
                    constraint_style.line_style_dict,
                    constraint_style.area_style_dict,
                    constraint_style.line_label,
                )
        else:
            LOGGER.info(f"plotting: {y.name} {kind} {m:+.3f} * {x.name} {b:+.3f}")
            add_constraint_from_slope_intercept(
                ax,
                m,
                b,
                kind,
                constraint_style.line_style_dict,
                constraint_style.area_style_dict,
                constraint_style.line_label,
            )

    add_optimal_marker(ax, x, y)

    ax.grid(True)
    make_key(ax)
    fig.tight_layout()

    return fig, ax


def plot_variable_bounds(ax: Axes, x: pulp.LpVariable, y: pulp.LpVariable):
    variable_limit_style = VariableLimitStyle()
    l_style = variable_limit_style.line_style_dict
    a_style = variable_limit_style.area_style_dict
    if x.lowBound is not None:
        add_constraint_at_x(ax, x.lowBound, ">=", l_style, a_style)
    if x.upBound is not None:
        add_constraint_at_x(ax, x.upBound, "<=", l_style, a_style)
    ax.set_xlabel(x.name)
    if y.lowBound is not None:
        add_constraint_at_y(ax, y.lowBound, ">=", l_style, a_style)
    if y.upBound is not None:
        add_constraint_at_y(ax, y.upBound, "<=", l_style, a_style)
    ax.set_ylabel(y.name)


def from_json(src: str, X: str, Y: str):
    _, problem = pulp.LpProblem.from_json(src)
    problem.solve(solver=pulp.getSolver("PULP_CBC_CMD"))
    # normalize calsim conventions
    if problem.objective:
        problem.objective = -problem.objective
    problem.sense *= -1
    fig, ax = plot_problem(X, Y, problem=problem)
    plt.show()
