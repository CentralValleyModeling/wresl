import matplotlib.pyplot as plt
import numpy as np
from matplotlib.axes import Axes
import pulp

from .style import lighten_color
from .logs import get_logger


LOGGER = get_logger(__name__)


def _add_constraint_from_xy(
    ax: Axes,
    x: np.ndarray,
    y: np.ndarray,
    kind: str,
    line_kwargs: dict,
    area_kwargs: dict,
    label: str | None = None,
):
    min_x, max_x = ax.get_xlim()
    min_y, max_y = ax.get_ylim()
    (line,) = ax.plot(x, y, **line_kwargs)
    if "<" in kind:
        ax.fill_between(x, y, [max_y for _ in x], **area_kwargs)
    elif ">" in kind:
        ax.fill_between(x, [min_y for _ in x], y, **area_kwargs)

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
        text_color = lighten_color(line.get_color(), 1.2)
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
    line_kwargs: dict,
    area_kwargs: dict,
    label: str | None = None,
):
    LOGGER.debug(f"plotting:  y {kind} {slope} * x + {b}")
    min_x, max_x = ax.get_xlim()
    x = np.linspace(min_x, max_x, 100)
    y = (slope * x) + b
    _add_constraint_from_xy(ax, x, y, kind, line_kwargs, area_kwargs, label)


def add_constraint_at_x(
    ax: Axes,
    x: float,
    kind: str,
    line_kwargs: dict,
    area_kwargs: dict,
    label: str | None = None,
):
    LOGGER.debug(f"plotting: x {kind} {x}")
    min_y, max_y = ax.get_ylim()
    min_x, max_x = ax.get_xlim()
    ys = np.linspace(min_y, max_y, 100)
    xs = np.array(tuple(x for _ in ys))
    (line,) = ax.plot(xs, ys, **line_kwargs)
    if "<" in kind:
        ax.fill_betweenx(ys, x, max_x, **area_kwargs)
    elif ">" in kind:
        ax.fill_betweenx(ys, min_x, x, **area_kwargs)
    if label is not None:
        # filter x and y to find plottable mid-point
        xy = tuple(
            (xi, yi)
            for xi, yi in zip(xs, ys)
            if (max_x > xi > min_x) and (max_y > yi > min_y)
        )
        # Add label
        dx = xy[-1][0] - xy[0][0]
        dy = xy[-1][1] - xy[0][1]
        # Rotation needs to be done in paper space
        x_label = xy[0][0] + (dx / 4)
        y_label = xy[0][1] + (dy / 4)
        text_color = lighten_color(line.get_color(), 1.2)
        plt.annotate(
            label.strip() + "\n",  # this is a cheat because va="baseline" isn't working
            (x_label, y_label),
            rotation=90,
            ha="center",
            va="center",
            color=text_color,
        )


def add_constraint_at_y(
    ax: Axes,
    y: float,
    kind: str,
    line_kwargs: dict,
    area_kwargs: dict,
    label: str | None = None,
):
    LOGGER.debug(f"plotting: y {kind} {y}")
    min_x, max_x = ax.get_xlim()
    xs = np.linspace(min_x, max_x, 100)
    ys = np.array(tuple(y for _ in xs))
    _add_constraint_from_xy(ax, xs, ys, kind, line_kwargs, area_kwargs, label)


def calc_interactivity(
    constraint: pulp.LpConstraint,
    x: pulp.LpVariable,
    y: pulp.LpVariable,
) -> int:
    interactivity = 0
    if x in constraint.expr:
        interactivity += 1
    if y in constraint.expr:
        interactivity += 1
    return interactivity
