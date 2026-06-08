import matplotlib.pyplot as plt
import numpy as np

from matplotlib.axes import Axes
from matplotlib.figure import Figure
from matplotlib.lines import Line2D
import pulp
import logging


LOGGER = logging.getLogger(__name__)
LOGGER.setLevel(logging.INFO)
logging.basicConfig(
    format="%(asctime)s %(levelname)5s - %(message)s",
)

SENSE_MAP = {
    1: "<=",
    -1: ">=",
    0: "=",
}


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
            *variables,
            problem.objective,
            problem.sense,
            *constraints,
        )
    else:
        raise ValueError("How did we get here?")
    return fig, ax


def zoom_1d(lo, hi, zoom=0.9) -> tuple[float, float]:
    span = hi - lo
    mid = lo + (span / 2)
    new_half_span = (span / zoom) / 2
    new_lo = mid - new_half_span
    new_hi = mid + new_half_span
    LOGGER.debug(f"zoomed from {lo} - {hi} to {new_lo} - {new_hi}")
    return new_lo, new_hi


def get_bounds(v: pulp.LpVariable):
    low = v.lowBound
    if v.lowBound is None:
        LOGGER.debug(f"assuming 0 for {v.name} lower bound while plotting")
        low = 0
    high = v.upBound
    if v.upBound is None:
        val = v.value()
        if val is not None:
            if val == v.lowBound:
                LOGGER.debug(
                    f"assuming 1 for {v.name} upper bound"
                    + " because solved value is 0"
                )
                high = 1
            else:
                LOGGER.debug(f"for {v.name} upper bound, assuming 2x the solved value")
                high = val * 2
        else:
            LOGGER.debug(f"for {v.name} upper bound, assuming 1,000,000")
            high = 1_000_000
    return zoom_1d(low, high, 0.9)


def plot_problem_2d(
    x: pulp.LpVariable,
    y: pulp.LpVariable,
    objective: pulp.LpAffineExpression,
    kind_magic_num: int,
    *constraints: pulp.LpConstraint,
) -> tuple[Figure, Axes]:
    LOGGER.debug("plotting 2D slice of a problem")
    fig, ax = plt.subplots(figsize=(6, 6))
    # Set bounds of plot
    xlo, xhi = get_bounds(x)
    ylo, yhi = get_bounds(y)

    ax.set_xlim(xlo, xhi)
    ax.set_ylim(ylo, yhi)
    # Variables
    if x.lowBound is not None:
        add_constraint_at_x(ax, x.lowBound, ">=", color="r")
    if x.upBound is not None:
        add_constraint_at_x(ax, x.upBound, "<=", color="r")
    ax.set_xlabel(x.name)
    if y.lowBound is not None:
        add_constraint_at_y(ax, y.lowBound, ">=", color="r")
    if y.upBound is not None:
        add_constraint_at_y(ax, y.upBound, "<=", color="r")
    ax.set_ylabel(y.name)
    # Add constraints
    for constraint in constraints:
        if (x not in constraint.expr) and (y not in constraint.expr):
            continue
        m, b = calc_slope_intercept(x, y, constraint)
        color = "xkcd:blue"
        linestyle = "-"
        kind = SENSE_MAP[constraint.sense]
        if constraint.sense == 0:
            color = "xkcd:cyan"
            linestyle = ":"

        if m == 0:
            try:
                _ = constraint.expr[x]
                LOGGER.info(f"plotting x limit: {x.name} {kind} {b}")
                add_constraint_at_x(ax, b, kind, color=color, label=constraint.name)
            except Exception:
                LOGGER.info(f"plotting y limit: {y.name} {kind} {b}")
                add_constraint_at_y(ax, b, kind, color=color, label=constraint.name)
        else:
            LOGGER.info(f"plotting: {y.name} {kind} {m:+.3f} * {x.name} {b:+.3f}")
            add_constraint_from_slope_intercept(
                ax, m, b, kind, color=color, linestyle=linestyle, label=constraint.name
            )

    add_optimal_marker(ax, x, y)

    ax.grid(True)

    make_key(ax)

    fig.tight_layout()

    return fig, ax


def make_key(ax: Axes):
    solution = Line2D([0], [0], color="g", marker="o")
    var_limit = Line2D([0], [0], color="r")
    equality_limit = Line2D([0], [0], color="xkcd:cyan", linestyle=":")
    bound_limit = Line2D([0], [0], color="xkcd:blue")
    ax.legend(
        [solution, var_limit, equality_limit, bound_limit],
        ["Solution", "Variable Limit", "Equality Constraint", "Regular Constraint"],
        bbox_to_anchor=(0.5, 0),
        loc="lower center",
        ncol=2,
    )


def add_optimal_marker(
    ax: Axes,
    x: pulp.LpVariable,
    y: pulp.LpVariable,
) -> Line2D:
    LOGGER.info(
        "marking the solved optimal value: "
        + f"{x.name}={x.value()}, {y.name}={y.value()}"
    )
    (point,) = ax.plot(x.value() or 0, y.value() or 0, "go")
    return point


def calc_slope_intercept(
    x: pulp.LpVariable,
    y: pulp.LpVariable,
    expression: pulp.LpConstraint,
):
    LOGGER.info(f"reducing constraint '{expression.name}': {expression}")
    if y not in expression.keys():
        LOGGER.debug(f"constraint is not a function of {y.name}")
        return _calc_intercept_for_1_var(x, expression)
    elif x not in expression.keys():
        LOGGER.debug(f"constraint is not a function of {x.name}")
        return _calc_intercept_for_1_var(y, expression)
    else:
        return _calc_slope_intercept_2_vars(x, y, expression)


def _calc_intercept_for_1_var(v: pulp.LpVariable, expression: pulp.LpConstraint):
    c: float = -expression.constant
    v_const = 1.0
    var: pulp.LpVariable
    for var, const in expression.items():
        if var is v:
            v_const = const
        else:
            LOGGER.debug(
                f"treating variable as solved constant: {var.name}={var.value()}"
            )
            c -= var.value() * const
    c = c / v_const
    if v_const < 0:
        kind = SENSE_MAP[-expression.sense]
    else:
        kind = SENSE_MAP[expression.sense]
    LOGGER.debug(f"constraint reduced to: {v.name} {kind} {c:+f}")
    return 0.0, c


def _calc_slope_intercept_2_vars(
    x: pulp.LpVariable,
    y: pulp.LpVariable,
    expression: pulp.LpConstraint,
):
    b: float = -expression.constant
    m = 1.0
    y_const = 1.0
    var: pulp.LpVariable
    for var, const in expression.items():
        if var is x:
            m = m * -const
        elif var is y:
            m = m / const
            y_const = const
        else:
            LOGGER.debug(
                "non-target variable in expression, "
                + f"treating as solved constant: {var.name}={var.value()}"
            )
            b -= var.value() * const
    b = b / y_const
    if y_const < 0:
        kind = SENSE_MAP[-expression.sense * -1]
    else:
        kind = SENSE_MAP[-expression.sense]
    LOGGER.debug(f"constraint reduced to: {y.name} {kind} {m} * {x.name} {b:+f}")
    return m, b


def add_constraint_from_slope_intercept(
    ax: Axes,
    slope: float,
    b: float,
    kind: str,
    **user_kwargs,
):
    LOGGER.debug(f"plotting:  y {kind} {slope} * x + {b}")
    min_x, max_x = ax.get_xlim()
    x = np.linspace(min_x, max_x, 100)
    y = (slope * x) + b
    kwargs = dict(
        linestyle="-",
        label=f"y {kind} {slope} * x + {b}",
    )
    kwargs.update(user_kwargs)
    _add_constraint_from_xy(ax, x, y, kind, **kwargs)


def _add_constraint_from_xy(
    ax: Axes,
    x: np.ndarray,
    y: np.ndarray,
    kind: str,
    label: str | None = None,
    **kwargs,
):
    min_x, max_x = ax.get_xlim()
    min_y, max_y = ax.get_ylim()
    (line,) = ax.plot(x, y, **kwargs)
    if "<" in kind:
        ax.fill_between(x, y, [max_y for _ in x], alpha=0.5, zorder=1, **kwargs)
    elif ">" in kind:
        ax.fill_between(x, [min_y for _ in x], y, alpha=0.5, zorder=1, **kwargs)

    if label is not None:
        # filter x and y to find plottable mid-point
        xy = tuple(
            (xi, yi)
            for xi, yi in zip(x, y)
            if (max_x > xi > min_x) and (max_y > yi > min_y)
        )
        # Add label
        dx = xy[-1][0] - xy[0][0]
        dy = xy[-1][1] - xy[0][1]
        # Rotation needs to be done in paper space
        scale = (max_y - min_y) / (max_x - min_x)
        dx_paper = dx * scale
        rot = np.rad2deg(np.arctan2(dy, dx_paper))
        x_label = xy[0][0] + (dx / 4)
        y_label = xy[0][1] + (dy / 4)
        text_color = lighten_color(line.get_color(), 1.5)
        plt.annotate(
            label.strip() + "\n",  # this is a cheat because va="baseline" isn't working
            (x_label, y_label),
            rotation=rot,
            ha="center",
            va="center",
            color=text_color,
        )


# From https://stackoverflow.com/questions/37765197/darken-or-lighten-a-color-in-matplotlib
def lighten_color(color, amount=0.5):
    """
    Lightens the given color by multiplying (1-luminosity) by the given amount.
    Input can be matplotlib color string, hex string, or RGB tuple.

    Examples:
    >> lighten_color('g', 0.3)
    >> lighten_color('#F034A3', 0.6)
    >> lighten_color((.3,.55,.1), 0.5)
    """
    import matplotlib.colors as mc
    import colorsys

    try:
        c = mc.cnames[color]
    except:
        c = color
    c = colorsys.rgb_to_hls(*mc.to_rgb(c))
    lightness = 1 - amount * (1 - c[1])
    if lightness > 1:
        LOGGER.warning(f"truncating lightness to white: {lightness=}")
        lightness = 1
    elif lightness < 0:
        LOGGER.warning(f"truncating lightness to black: {lightness=}")
        lightness = 0
    return colorsys.hls_to_rgb(c[0], lightness, c[2])


def add_constraint_at_x(
    ax: Axes,
    x: float,
    kind: str,
    **kwargs,
):
    LOGGER.debug(f"plotting: x {kind} {x}")
    min_y, max_y = ax.get_ylim()
    min_x, max_x = ax.get_xlim()
    ys = np.linspace(min_y, max_y, 100)
    xs = np.array(tuple(x for _ in ys))
    (line,) = ax.plot(xs, ys, **kwargs)
    if "<" in kind:
        ax.fill_betweenx(ys, x, max_x, alpha=0.5, zorder=1, **kwargs)
    elif ">" in kind:
        ax.fill_betweenx(ys, min_x, x, alpha=0.5, zorder=1, **kwargs)


def add_constraint_at_y(
    ax: Axes,
    y: float,
    kind: str = ">=",
    **kwargs,
):
    LOGGER.debug(f"plotting: y {kind} {y}")
    min_x, max_x = ax.get_xlim()
    xs = np.linspace(min_x, max_x, 100)
    ys = np.array(tuple(y for _ in xs))
    _add_constraint_from_xy(ax, xs, ys, kind, **kwargs)


def add_obj_gradient(ax: Axes, x_weight: float, y_weight: float, **user_kwargs):
    LOGGER.info(f"plotting: obj = {x_weight} * x + {y_weight} * y")
    min_x, max_x = ax.get_xlim()
    min_y, max_y = ax.get_ylim()
    xs = np.linspace(min_x, max_x, 100)
    ys = np.linspace(min_y, max_y, 100)
    X, Y = np.meshgrid(xs, ys)
    Z = (X * x_weight) + (Y * y_weight)
    kwargs = dict(
        levels=10,
        cmap="Purples",
        linewidths=1,
        alpha=0.5,
        zorder=0,
    )
    kwargs.update(user_kwargs)
    ax.contourf(X, Y, Z, **kwargs)


def add_obj_vector(ax: Axes, x_weight: float, y_weight: float, **user_kwargs):
    LOGGER.info(f"plotting gradient for: obj = {x_weight} * x + {y_weight} * y")
    min_x, max_x = ax.get_xlim()
    min_y, max_y = ax.get_ylim()

    xs = np.linspace(min_x, max_x, 11)
    ys = np.linspace(min_y, max_y, 11)
    X, Y = np.meshgrid(xs, ys)
    U = np.ones(shape=X.shape) * x_weight
    V = np.ones(shape=Y.shape) * y_weight
    kwargs = dict(
        alpha=0.1,
        zorder=0,
        color="xkcd:purple",
        angles="uv",
    )
    kwargs.update(user_kwargs)

    ax.quiver(X, Y, U, V, **kwargs)


def from_json(src: str, X: str, Y: str):
    _, problem = pulp.LpProblem.from_json(src)
    problem.solve(solver=pulp.getSolver("PULP_CBC_CMD"))
    # normalize calsim conventions
    problem.objective = -problem.objective
    problem.sense *= -1
    fig, ax = plot_problem(X, Y, problem=problem)
    plt.show()


def tutorials_index_01():
    problem: pulp.LpProblem = pulp.LpProblem(
        "Mass Balance Example",
        pulp.LpMaximize,
    )
    # Constraints
    OUTFLOW = problem.add_variable("OUTFLOW", 0, None)
    DELIVERY = problem.add_variable("DELIVERY", 0, 50)
    # Objective
    problem += (10 * DELIVERY) + (1 * OUTFLOW)
    # Constraints
    problem += OUTFLOW >= (0.25 * DELIVERY) + 25, "MINIMUM_FLOW_REQUIREMENT"
    problem += 60 - OUTFLOW - DELIVERY == 0, "MASS_BALANCE"

    LOGGER.info("solving pulp problem".center(50, "."))
    problem.solve(solver=pulp.getSolver("PULP_CBC_CMD"))

    LOGGER.info("pulp problem done solving".center(50, "."))
    # Plot things
    fig, ax = plot_problem("DELIVERY", "OUTFLOW", problem=problem)
    plt.show()


# if __name__ == "__main__":
#     tutorials_index_01()
