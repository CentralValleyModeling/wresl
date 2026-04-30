.. _tutorials-index:

.. role:: wresl(code)
   :language: wresl

Tutorials
=========

*Learning-oriented: step-by-step lessons for beginners.*

WRESL+ source code helps the modeler do two important things:

1. Define the execution structure of your study.
2. Define the network, constraints, and goals for your study.

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
        lower 0
        upper 100
        units 'CFS'
        kind 'FLOW'
    }

    goal MASS_BALANCE {
        INFLOW - OUTFLOW = 0
    }

The tutorials listed below go into a little more detail:

.. toctree::
   :maxdepth: 1
   :glob:

   **