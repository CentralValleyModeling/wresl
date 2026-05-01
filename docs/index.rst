WRESL+
======

.. _GitHub Discussions: https://github.com/CentralValleyModeling/wresl/discussions
.. _GitHub Issues: https://github.com/CentralValleyModeling/wresl/issues

Introduction
~~~~~~~~~~~~

WRESL+ is a domain-specific language for water resources modeling, used 
within the WRIMS (Water Resources Integrated Modeling System). It 
enables users to define state variables, decision variables, goals, and
optimization objectives for complex water resource simulations. WRESL+ 
is designed to support reproducible, modular, and transparent modeling 
for hydrologic and operations studies.

.. note::
    This documentation is under active development.

This site contains the documentation for the language. See below for 
the various resources:

+-------------+---------------------------------------+------------------------------------+
|             | *Acquisition*                         | *Goals*                            |
+-------------+---------------------------------------+------------------------------------+
| *Action*    | :ref:`Tutorials<tutorials-index>`     | :ref:`How-To Guides<how-to-index>` |
+-------------+---------------------------------------+------------------------------------+
| *Cognition* | :ref:`Explanation<explanation-index>` | :ref:`Reference<reference-index>`  |
+-------------+---------------------------------------+------------------------------------+

:ref:`Tutorials<tutorials-index>`
---------------------------------

Tutorials are intended to help new users of WRESL+ to learn the basics. 
These tutorials are not a complete learning experience; learners will 
likely need to refresh their understanding of WRIMS, linear programming,
and water resources concepts as they think about these tutorials.

.. note::

    These tutorials are being expanded. If you have ideas for new ways 
    of learning WRESL+, please let us know via our `GitHub Discussions`_!

:ref:`How-To Guides<how-to-index>`
----------------------------------

How-To guides are intended to help users complete common workflows. 
These are not focused on learning, but more on documenting the preferred
styles for using the language. 

.. note::

    Some workflows here are not the **only** way to complete these 
    tasks, and there are good reasons to do things differently than how 
    we have presented them here. If you think we are suggesting bad 
    practice, please let us know (`GitHub Issues`_).

:ref:`Explanation<explanation-index>`
-------------------------------------

These explanations are give a greater understanding of how WRESL+ was 
designed, how it fits within the WRIMS toolkit.

:ref:`Reference<reference-index>`
---------------------------------

The reference section is intended to be a repository of detailed 
information useful mostly to those already familair with WRESL+, and 
need quick reminders of very specific information.

Legacy documentation
--------------------

.. _WRESL Language Reference: https://data.cnra.ca.gov/dataset/wrims-2-gui/resource/44ce6f9c-c7e5-48c1-8d3a-930d0059547e
.. _WRESL Plus Language Reference: https://data.cnra.ca.gov/dataset/wrims-2-gui/resource/93b6243c-57eb-47b5-ad62-7209c0acdfb6

This website is attempting to replace the legacy PDF documents that were 
previously used to document WRESL+. If you think we are missing 
something, please let us know! If you still need to reference those 
older materials, they are linked below:

- `WRESL Language Reference`_
- `WRESL Plus Language Reference`_

Site Contents
-------------

.. toctree::
   :maxdepth: 1
   :glob:
   
   tutorials/index
   how-to/index
   explanation/index
   reference/index
