from matplotlib.axes import Axes
from matplotlib.lines import Line2D
import pulp

from .logs import get_logger

LOGGER = get_logger(__name__)


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


def make_key(ax: Axes):
    solution = Line2D([0], [0], color="g", marker="o", linestyle="")
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
