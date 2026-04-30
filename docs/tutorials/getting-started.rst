.. _tutorials-getting-started:

.. role:: wresl(code)
   :language: wresl


Getting Started with WRESL+
===========================

Prerequisites
~~~~~~~~~~~~~

.. _official source: https://water.ca.gov/Library/Modeling-and-Analysis/Modeling-Platforms/Water-Resource-Integrated-Modeling-System

- Download and install the WRIMS GUI from the `official source`_.
- Familiarize yourself with the WRIMS IDE.

Your First Model
~~~~~~~~~~~~~~~~

This tutorial walks through a simple study adapted from the WRESL+ Language Reference (2018) Quick Start example.

1. Create a main file ``main.wresl`` with sequences, models, variables, goals, and an objective:
   
.. code-block:: wresl
   :linenos:

   sequence CYCLE1 { 
      model First 
      order 1 
   }
   
   sequence CYCLE2 { 
      model Second 
      order 2 
   }

   model First {
      define I01 {
         timeseries 
         kind 'FLOW-INFLOW' 
         units 'TAF' 
      }
      
      dvar X1 { 
         std 
         kind 'FLOW-CHANNEL' 
         units 'CFS' 
      }
      dvar X2 {
         std 
         kind 'FLOW-CHANNEL' 
         units 'CFS' 
      }
      svar C { value I02 * 5 }
      
      goal Test { X1 + X2 < C }
      
      include 'allocation\test1.wresl'
      
      objective XGroup {
         [ X1, 10 ]
         [ X2, 20 ]
      }
   }

   model Second {
      define I02 { 
         timeseries 
         kind 'FLOW-INFLOW' 
         units 'TAF'
      }
      dvar Y1 { 
         std 
         kind 'FLOW-CHANNEL' 
         units 'CFS'
      }
      dvar Y2 { 
         std 
         kind 'FLOW-CHANNEL' 
         units 'CFS'
      }
      svar X1_Upstream { 
         value X1[First] 
      }
      
      goal Test2 { Y1 + Y2 < I02 + X1_Upstream }
      
      include 'allocation\test2.wresl'
      
      objective YGroup {
         [ Y1, 10 ]
         [ Y2, 20 ]
      }
   }
   
2. Run the model using the WRIMS GUI. (Check out the :ref:`WRIMS GUI<wrims-gui:home>` docs)
3. Examine the output: the solver determines ``X1``, ``X2``, ``Y1``, and ``Y2`` values that satisfy the constraints while optimizing the weighted objectives.


What Just Happened?
~~~~~~~~~~~~~~~~~~~

First, the `sequence` / `model` / `include` structure told WRIMS which files to load and in what order.
 
Within the ``First`` model, you defined 4 different variables. 

Two of them were **State Variables**

1. :wresl:`define I01 { timeseries kind 'FLOW-INFLOW' units 'TAF' }`
2. :wresl:`svar C { value I02*5 }` 


Two of them were **Decision Variables**

3. :wresl:`dvar X1 { std kind 'FLOW-CHANNEL' units 'CFS' }` 
4. :wresl:`dvar X2 { std kind 'FLOW-CHANNEL' units 'CFS' }` 

    
You also created a **constraint** (in the MILP sense of the word), that the solver will satisfy when it optimizes the objective function. 

:wresl:`goal Test { X1 + X2 < C }` 

Speaking of the objective function, you also created that within the ``First`` model.

:wresl:`objective XGroup { [X1, 10] [X2, 20] }` 

Within the ``Second`` model, you did pretty much the same thing as in the ``First`` model, except...

:wresl:`svar X1_Upstream { value X1[First] }` 

You referenced the result of `X1` from the `First` sequence, demonstrating **cross-cycle variable access**.

