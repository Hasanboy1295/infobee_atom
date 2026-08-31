"""Train the ATOM / EDBO direct-arylation yield ML model.

Reads the EDBO experiment index CSV, builds molecular-composition descriptors
from the SMILES columns (plus temperature and concentration), trains a
HistGradientBoostingRegressor and writes the artifact used by main.py to
`ai-service/atom_model.joblib`.

Usage:
    python train_model.py [path/to/edbo_direct_arylation_experiment_index.csv]
"""

import csv
import os
import re
import sys

import joblib
import numpy as np
from sklearn.ensemble import HistGradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split

DEFAULT_CSV = "/Users/khan/Desktop/work/infobee/ATOM_RESEARCH_DATA_20260824/01_public_validation/edbo_direct_arylation_experiment_index.csv"
OUTPUT = os.path.join(os.path.dirname(__file__), "atom_model.joblib")
VERSION = "edbo-hgb-1.0.0"

ELEMENTS = ["C", "N", "O", "P", "S", "F", "Cl", "Br", "I", "B", "Si", "K", "Na", "Li"]
TOKEN_RE = re.compile(r"Cl|Br|[A-Z][a-z]?|[a-z]")
MASS = {"C": 12, "N": 14, "O": 16, "P": 31, "S": 32, "F": 19, "Cl": 35.5,
        "Br": 80, "I": 127, "B": 11, "Si": 28, "K": 39, "Na": 23, "Li": 7}


def elem_counts(smiles):
    counts = {e: 0 for e in ELEMENTS}
    if not smiles:
        return counts
    for tok in TOKEN_RE.findall(smiles):
        if tok in counts:
            counts[tok] += 1
    return counts


def approx_mass(counts):
    return sum(MASS.get(e, 0) * n for e, n in counts.items())


def build_features(base, lig, solv, temp, conc):
    """Mirror the feature construction used in main.py._build_ml_features."""
    return {
        "base_mass": approx_mass(base), "base_C": base["C"], "base_N": base["N"],
        "base_O": base["O"], "base_P": base["P"], "base_S": base["S"],
        "base_hal": base["F"] + base["Cl"] + base["Br"] + base["I"],
        "lig_mass": approx_mass(lig), "lig_C": lig["C"], "lig_N": lig["N"],
        "lig_O": lig["O"], "lig_P": lig["P"], "lig_S": lig["S"],
        "lig_hal": lig["F"] + lig["Cl"] + lig["Br"] + lig["I"],
        "solv_mass": approx_mass(solv), "solv_C": solv["C"], "solv_N": solv["N"],
        "solv_O": solv["O"], "solv_hal": solv["F"] + solv["Cl"] + solv["Br"] + solv["I"],
        "temp": temp, "conc": conc,
    }


def main(argv=None):
    argv = argv if argv is not None else sys.argv[1:]
    csv_path = argv[0] if argv else DEFAULT_CSV

    rows = []
    with open(csv_path, newline="") as f:
        for r in csv.DictReader(f):
            try:
                yield_val = float(r["yield"])
                temp = float(r["Temp_C"])
                conc = float(r["Concentration"])
            except (ValueError, TypeError):
                continue
            feats = build_features(
                elem_counts(r.get("Base_SMILES", "")),
                elem_counts(r.get("Ligand_SMILES", "")),
                elem_counts(r.get("Solvent_SMILES", "")),
                temp, conc,
            )
            rows.append((feats, yield_val))

    if len(rows) < 50:
        raise SystemExit(f"Too few usable rows ({len(rows)}) in {csv_path}")

    feature_names = sorted(rows[0][0].keys())
    X = np.array([[r[0][k] for k in feature_names] for r in rows], dtype=float)
    y = np.array([r[1] for r in rows], dtype=float)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    model = HistGradientBoostingRegressor(random_state=42)
    model.fit(X_train, y_train)

    pred = model.predict(X_test)
    mae = mean_absolute_error(y_test, pred)
    r2 = r2_score(y_test, pred)
    baseline = mean_absolute_error(y_test, np.full_like(y_test, y_train.mean()))

    artifact = {
        "model": model,
        "feature_names": feature_names,
        "model_name": "HistGradientBoostingRegressor",
        "train_mae": float(mae),
        "train_r2": float(r2),
        "baseline_mae": float(baseline),
        "n_samples": len(rows),
        "version": VERSION,
    }
    joblib.dump(artifact, OUTPUT)
    print(f"rows={len(rows)}  MAE={mae:.3f}  R2={r2:.3f}  baseline_mae={baseline:.3f}")
    print(f"saved -> {OUTPUT}  ({os.path.getsize(OUTPUT)} bytes)")


if __name__ == "__main__":
    main()
