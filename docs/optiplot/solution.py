import numpy as np
import pulp
from matplotlib.axes import Axes
from matplotlib.lines import Line2D

from .logs import get_logger

LOGGER = get_logger(__name__)


def add_optimal_marker(
    ax: Axes,
    x: pulp.LpVariable,
    y: pulp.LpVariable,
) -> Line2D:
    LOGGER.info(
        "marking the solved optimal value: "
        + f"{x.name}={x.value()}, {y.name}={y.value()}"
    )
    px = x.value() or 0
    py = y.value() or 0
    (point,) = ax.plot(px, py, "go")
    ax.annotate(
        f"({round(px)}, {round(py)})",
        (px, py),
        (0, 1),
        ha="center",
        textcoords="offset fontsize",
        color="green",
    )
    return point


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
