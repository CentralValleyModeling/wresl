import matplotlib.pyplot as plt
import numpy as np
from matplotlib.axes import Axes

from .style import lighten_color
from .logs import get_logger

LOGGER = get_logger(__name__)


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
    kind: str,
    **kwargs,
):
    LOGGER.debug(f"plotting: y {kind} {y}")
    min_x, max_x = ax.get_xlim()
    xs = np.linspace(min_x, max_x, 100)
    ys = np.array(tuple(y for _ in xs))
    _add_constraint_from_xy(ax, xs, ys, kind, **kwargs)
