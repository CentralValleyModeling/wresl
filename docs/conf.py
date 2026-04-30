# Configuration file for the Sphinx documentation builder.

# -- Project information

project = "WRESL+"
copyright = "2026, CA Department of Water Resources"
author = "Zachary Roy"

release = "0.1"
version = "0.1.0"

# -- General configuration

extensions = [
    "sphinx.ext.duration",
    "sphinx.ext.doctest",
    "sphinx.ext.autodoc",
    "sphinx.ext.autosummary",
    "sphinx.ext.intersphinx",
    "myst_parser",
]

intersphinx_mapping = {
    "wrims-engine": (
        "https://wrims-docs.readthedocs.io/projects/wrims-engine/en/latest",
        None,
    ),
    "wrims-gui": (
        "https://wrims-docs.readthedocs.io/projects/wrims-gui/en/latest",
        None,
    ),
    "wrims-docs": (
        "https://wrims-docs.readthedocs.io/en/latest",
        None,
    ),
}

myst_heading_anchors = 4

templates_path = ["_templates"]

# -- Options for HTML output

html_theme = "sphinx_rtd_theme"

# -- Options for EPUB output
epub_show_urls = "footnote"
