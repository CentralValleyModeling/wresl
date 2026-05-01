.. _tutorials-shasta-mass-balance:

.. role:: wresl(code)
   :language: wresl

Shasta Reservoir Mass Balance
-----------------------------

This example demonstrates a realistic reservoir mass balance using actual variable definitions and the continuity goal from CalSim3.

First, the variables are declared across system files:

.. code-block:: wresl

   ! State variable: known input
   define I_SHSTA {timeseries kind 'INFLOW' units 'TAF' convert 'CFS'}   ! Sacramento River inflow

   ! Decision variables
   define S_SHSTA       {std kind 'STORAGE' units 'TAF'}                 ! reservoir storage
   define S_SHSTAlevel1 {value 550}                                      ! dead pool (TAF)
   define S_SHSTAlevel6 {value 4552}                                     ! full pool (TAF)
   define C_SHSTA       {std kind 'CHANNEL' units 'CFS'}                 ! channel flow below dam
   define D_SHSTA_WTPJMS {std kind 'DIVERSION' units 'CFS'}              ! diversion to water treatment plant
   define E_SHSTA       {lower unbounded kind 'EVAPORATION' units 'CFS'} ! evaporation loss
   

Then the continuity constraint ties them together:

.. code-block:: wresl

   ! Mass balance at Shasta: inflow - outflows = change in storage
   goal continuitySHSTA {
      I_SHSTA - C_SHSTA - D_SHSTA_WTPJMS - E_SHSTA = S_SHSTA * taf_cfs - S_SHSTA(-1) * taf_cfs + 0.
   }


**Key points:**

- CalSim models conventionally use **CFS** (cubic feet per second) for flows and **TAF** (thousand acre-feet) for storage. The built-in conversion factor `taf_cfs` converts TAF to CFS for the current time step, ensuring consistent units within constraints.
- :wresl:`S_SHSTA(-1)` references the storage from the previous time step.
- The `+ 0.` at the end is a CalSim convention for consistency in the continuity equations.

### Tutorial: Conditional Operations with Case/Condition
Real-world operations often depend on conditions such as hydrology, time of year, or regulatory requirements. This example from the WRESL+ Language Reference (2018) assigns values based on the current month:

.. code-block:: wresl

   svar NMTest {
      case February {
         condition month == feb
         value S10(-12) + sumI10_part
      }
      case Others {
         condition always
         value S10(prevfeb) + sumI10_part
      }
   }

Here is a real-world example from the CalSim3 study that determines CVP allocation based on water supply index:

.. code-block:: wresl

   define WSI_NOD_Alloc {
      case MarchAprilMay {
         condition month >= MAR .and. month <= MAY
         select Alloc from WSI_CVP_NODAg given WSI=WSI_CVP_NOD use linear where month=month
      }
      case otherwise {
         condition always
         value 0.
      }
   }


And another from CVP operations that sets seasonal balancing targets:

.. code-block:: wresl

   define S_WKYTN_Septarget {
      case dry {
         condition wyt_SAC == 5
         value 180
      }
      case other {
         condition always
         value 235
      }
   }


**Key points:**

- Conditions are evaluated top-to-bottom; the first match is used.
- Always end with :wresl:`condition always` as a default/fallback.
- You can have as many :wresl:`case` blocks as needed.

### Tutorial: Multi-File Model Organization
As models grow, splitting definitions across files keeps things manageable. Here is an excerpt from the actual CalSim3 main file showing how the study is organized:

**mainCS3_ReOrg_UWplusVF.wresl** (entry point — abbreviated):

.. code-block:: wresl

   SEQUENCE CYCLE33 {
      model   UPSTREAM
      condition simulateSacramentoValley >= 0.5
      order   33
   }
   SEQUENCE CYCLE35 {
      model   DELTA
      condition simulateSacramentoValley >= 0.5
      order   35
   }

   group Tdomain {
      define SGPHIGH {value 77777}
      define daysindv { alias daysin kind 'DAYS' units 'days'}
      define cfs_cfm  { value daysin * 86400.0}
   }

   group Common_All {
      include 'CVGroundwater\CalSim3GWregionIndex.wresl'
      include 'Other\NewFacilitySwitches.wresl'
      include 'Other\wytypes\WyTypesGeneral.wresl'
      include 'CVGroundwater\CVGroundwater_ExtFuncs.wresl'
      include 'NorthOfDelta\hydrology\forecast\forecast.wresl'
      include 'System\System_Sac_cycle1.wresl'
   }


**System/SystemTables_Sac/arcs-Reservoirs.wresl** (variable declarations):
.. code-block:: wresl

   define S_SHSTAlevel1 {value 550}
   define S_SHSTA_1     {std kind 'STORAGE-ZONE' units 'TAF'}
   define S_SHSTAlevel2 {timeseries kind 'STORAGE-LEVEL' units 'TAF'}
   define S_SHSTA_2     {std kind 'STORAGE-ZONE' units 'TAF'}
   define S_SHSTAlevel6 {value 4552}
   define S_SHSTA_6     {std kind 'STORAGE-ZONE' units 'TAF'}
   define S_SHSTA       {std kind 'STORAGE' units 'TAF'}           !SHASTA LAKE
   define E_SHSTA       {lower unbounded kind 'EVAPORATION' units 'CFS'}


**System/SystemTables_Sac/constraints-Connectivity.wresl** (goals):

.. code-block:: wresl

   goal continuitySHSTA  {
      I_SHSTA 
      - C_SHSTA 
      - D_SHSTA_WTPJMS 
      - E_SHSTA 
      =   
        S_SHSTA * taf_cfs 
      - S_SHSTA(-1) * taf_cfs 
      + 0.
   }

   goal continuityKSWCK  {
      C_SAC301 
      + SR_02_KSWCK 
      + SR_03_KSWCK 
      + R_03_PU1_KSWCK 
      - C_KSWCK 
      - E_KSWCK 
      =   
        S_KSWCK * taf_cfs 
      - S_KSWCK(-1) * taf_cfs 
      + 0.
   }


This pattern — separating variables from goals, organizing by geographic location and system component, and using :wresl:`include` with :wresl:`group` — is the standard way to organize WRESL+ models.
