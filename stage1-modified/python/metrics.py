"""Helpers for reporting the Stage 1 measurements required by the report."""

try:
    from .tokenizer import measure_storage
except ImportError:
    from tokenizer import measure_storage

__all__ = ["measure_storage"]
