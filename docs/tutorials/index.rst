.. _tutorials-index:

.. role:: wresl(code)
   :language: wresl

Tutorials
=========

.. toctree::
   :maxdepth: 1
   :glob:

   **

WRESL+ source code helps the modeler do two important things:

1. Define the execution structure of your study.
2. Define the network, constraints, and goals for your study.

Model Execution Structure
-------------------------

To define the execution structure of your study we mostly use 
``sequence``, ``model``, ``group``, and ``include``. These objects and 
directives tell the study what MILP problems to construct, and the order 
that they should be evaluated.

Below is an example that defines a 2-step study, which first solves a 
simple stream network problem, and then solves a more complex problem.

.. code-block:: wresl
    :linenos:

    model SimpleOperations {
        include group StreamNetwork  // the network structure is defined in this group
        include 'weights.wresl'  // this file has the MILP wieght definitions
    }

    model ComplexOperations {
        include group StreamNetwork
        include group OperationsDefinition  // in this "model" we also include operations
        include 'weights.wresl'
    }

    sequence First {
        model SimpleOperations
        condition always
        order 1  // the simple model goes first
    }

    sequence Second {
        model ComplexOperations
        condition always
        order 2  // the more complex model goes second
    }

Model Variables, Constraints, and Objective
-------------------------------------------

To define the network, constraints, and goals of the study, we mostly 
use `define`, and `goal` objects. These objects create variables, and 
add constraints to the study. 

Below is an example that enforces a very simple mass balance equation.  

.. code-block:: wresl
    :linenos:

    define INFLOW {
        timeseries
        units 'CFS'
        kind 'FLOW'
    }

    define OUTFLOW {
        std
        units 'CFS'
        kind 'FLOW'
    }

    define DELIVERY {
        lower 0
        upper 50
        units 'CFS'
        kind 'FLOW'
    }

    define BASE_FLOW {
        value 25
    }

    goal MASS_BALANCE {
        INFLOW - OUTFLOW - DELIVERY = 0
    }

    goal MINIMUM_FLOW_REQUIREMENT {
        OUTFLOW > (0.25 * DELIVERY) + BASE_FLOW
    }

    objective objAll = {
        [DELIVERY, 10],
        [OUTFLOW, 1]
    }

If the ``INFLOW`` term is equal to 60, then the problem above can be visualized as the plot below:

.. plot:: plots.py tutorials_index_01

Some things to note about this problem:

1. The variable bounds limit all solutions to the area not shaded red.
2. The mass balance constraint limits all solutions to the area above the blue region.
3. Since ``MASS_BALANCE`` uses an equality constraint, the solution must lie exactly on the cyan line.
4. Since ``DELIVERY`` has a larger weight (``10``) than ``OUTFLOW`` (``1``), the optimal value is at ``(28, 32)``. If the weight priority had been switched, the optimal value would be at ``(0, 60)``.