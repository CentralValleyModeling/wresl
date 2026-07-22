import numpy as np
import pulp
from matplotlib.axes import Axes
from matplotlib.lines import Line2D

from .logs import get_logger
from .style import SOLUTION_STYLE, SOLUTION_COLOR, lighten_color

LOGGER = get_logger(__name__)
import contextlib
import sys
from io import StringIO


@contextlib.contextmanager
def silenced(no_stdout=True, no_stderr=True):
    """
    Suppresses output to stdout and/or stderr.
    Always resets stdout and stderr, even on an exception.
    Usage:
        with silenced(): print("This doesn't print")
    Modified from post by Alex Martelli in https://stackoverflow.com/questions/2828953/silence-the-stdout-of-a-function-in-python-without-trashing-sys-stdout-and-resto/2829036#2829036
    which is licensed under CC BY-SA 3.0 https://creativecommons.org/licenses/by-sa/3.0/
    """
    if no_stdout:
        save_stdout = sys.stdout
        sys.stdout = StringIO()
    if no_stderr:
        save_stderr = sys.stderr
        sys.stderr = StringIO()
    yield
    if no_stdout:
        sys.stdout = save_stdout
    if no_stderr:
        sys.stderr = save_stderr


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
    (point,) = ax.plot(px, py, **SOLUTION_STYLE)
    ax.annotate(
        f"({round(px)}, {round(py)})",
        (px, py),
        (0, 1),
        ha="center",
        textcoords="offset fontsize",
        color=lighten_color(SOLUTION_COLOR, 0.8),
        zorder=98,
    )
    ax.annotate(
        f"({round(px)}, {round(py)})",
        (px, py),
        (0.02, 0.99),
        ha="center",
        textcoords="offset fontsize",
        color=lighten_color(SOLUTION_COLOR, 1.2),
        zorder=97,
    )
    return point


def add_naieve_gradient(ax: Axes, x_weight: float, y_weight: float, **user_kwargs):
    LOGGER.info(f"plotting: obj = {x_weight} * x + {y_weight} * y")
    min_x, max_x = ax.get_xlim()
    min_y, max_y = ax.get_ylim()
    xs = np.linspace(min_x, max_x, 100)
    ys = np.linspace(min_y, max_y, 100)
    X, Y = np.meshgrid(xs, ys)
    Z = (X * x_weight) + (Y * y_weight)
    kwargs = dict(
        levels=100,
        cmap="Greens",
        linecolor="none",
        alpha=0.5,
        zorder=0,
        antialiased=True,
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
